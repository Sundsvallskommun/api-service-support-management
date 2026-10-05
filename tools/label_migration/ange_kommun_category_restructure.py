#!/usr/bin/env python3
"""
KC Ånge (Ånge kommun) errand-category restructuring runbook.

Executes the full category-tree change described in the Jira ticket end to
end, against a running api-service-support-management instance's Metadata
Labels API (se.sundsvall.supportmanagement.api.MetadataLabelResource):

    PUT  /{municipalityId}/{namespace}/metadata/labels          (add/rename/delete)
    POST /{municipalityId}/{namespace}/metadata/labels/{id}/move
    POST /{municipalityId}/{namespace}/metadata/labels/{id}/merge

All CATEGORY/TYPE/SUBTYPE codes and display names below are taken verbatim
from the ticket's own tables (Ta bort / Lagg till / Namnbyte / Flytta +
migrera) - nothing here is a paraphrase or a re-derived summary. The tree
itself is only ever read from the live server and mutated in memory: no
production label id is hardcoded, since new labels do not have one yet and
existing ones are not known ahead of time.

Two things from the ticket are NOT automated here and must be resolved
before running against production:

  1. The "Ta bort" table lists both URBAN_DEVELOPMENT/PROPERTY/MAINTENANCE
     (reason: "Migreras") and URBAN_DEVELOPMENT/PLANNING/MAINTENANCE
     (reason: "-") for removal, but only the first has a migration
     destination in the "Flytta" table. Confirm with the ticket author
     whether the second is a duplicate/typo before including it - it is
     deliberately left out of DELETIONS below.
  2. The KC Ånge municipalityId and namespace values themselves - passed as
     required CLI arguments rather than guessed.

Usage:
    pip install requests

    # Dry run against every source label the script needs to touch, no writes:
    python3 ange_kommun_category_restructure.py \\
        --base-url https://<host> --municipality-id <id> --namespace <ns> \\
        --token-url <oauth2-token-url> --client-id <id> --client-secret <secret>

    # Execute for real, once the dry run output looks right:
    python3 ange_kommun_category_restructure.py \\
        --base-url https://<host> --municipality-id <id> --namespace <ns> \\
        --token-url <oauth2-token-url> --client-id <id> --client-secret <secret> \\
        --execute

Without --execute the script only prints what it would send (the tree PUT
body, and dry-run results for every move/merge) and makes no writes at all,
including the label-move/merge dry-run calls themselves (dryRun: true is
always safe - the server makes no changes for those either way, so those
calls do happen even without --execute to surface affected-errand counts
early).
"""

import argparse
import copy
import json
import sys
import time
from dataclasses import dataclass, field

import requests

# =============================================================================
# Literal ticket data
# =============================================================================

# (category, type, subtype_or_None, display_name)
# Pure additions only - destinations of a move+rename (ESTATES, REHABILITATION_UNIT)
# are NOT here, they are created by moving+renaming an existing label in step 3.
ADDITIONS = [
    ("SOCIAL_SERVICES", "CHILD_FAMILY", "VITALIA", "Vitalia"),
    ("SOCIAL_SERVICES", "MUNICIPAL_HEALTH_CARE", None, "Kommunal hälso- och sjukvård"),
    ("SOCIAL_SERVICES", "MUNICIPAL_HEALTH_CARE", "DISTRICT_NURSE", "Sjuksköterska/Distriktsköterska"),
    ("SOCIAL_SERVICES", "SUPPORT_CARE", "GROUP_HOMES", "Gruppbostäder"),
    ("SOCIAL_SERVICES", "SUPPORT_CARE", "PERSONAL_ASSISTANTS", "Personliga assistenter"),
    ("SOCIAL_SERVICES", "ELDERLY_CARE", None, "Äldreomsorg"),
    ("SOCIAL_SERVICES", "ELDERLY_CARE", "HOME_CARE_SERVICE", "Hemtjänst"),
    ("SOCIAL_SERVICES", "ELDERLY_CARE", "SPECIAL_HOUSING", "Särskilt boende"),
    ("SOCIAL_SERVICES", "ELDERLY_CARE", "STAFFING", "Bemanningen"),
    ("ADMINISTRATION", "HR", "OTHER_MATTERS", "Övriga frågor"),
]

# (category, type, subtype_or_None, new_display_name) - id/resourceName retained
RENAMES = [
    ("URBAN_DEVELOPMENT", "BUSINESS", None, "Näringsliv"),
    ("SOCIAL_SERVICES", "CHILD_FAMILY", "FINANCIAL_SUPPORT", "Försörjningsstöd / Ekonomiskt bistånd"),
]

# (category, type, subtype_or_None) - confirmed zero errands in the ticket
DELETIONS = [
    ("FINANCE_DEPARTMENT", "FINANCE", "DONATION_FUNDS"),
    ("FINANCE_DEPARTMENT", "FINANCE", "FUNDS"),
    ("SOCIAL_SERVICES", "CHILD_FAMILY", "PATERNITY"),
    ("SOCIAL_SERVICES", "CHILD_FAMILY", "VOLUNTEER_CENTER"),
    ("ADMINISTRATION", "OFFICE", "LEGAL_ADVICE"),
    # URBAN_DEVELOPMENT/PLANNING/MAINTENANCE deliberately NOT included - see module docstring.
]

# (source_path, dest_parent_path) - 1:1 subtree move, resourceName unchanged.
# Each *_path is a tuple of (category, type, subtype_or_None).
PLAIN_MOVES = [
    (("ADMINISTRATION", "CULTURE_RECREATION", None), ("URBAN_DEVELOPMENT",)),
    (("EDUCATION", "CULTURAL_SCHOOL", None), ("URBAN_DEVELOPMENT",)),
    (("SOCIAL_SERVICES", "SUPPORT_CARE", "BENEFITS_ASSESSMENT_LSS"), ("SOCIAL_SERVICES", "CHILD_FAMILY")),
    (("SOCIAL_SERVICES", "SUPPORT_CARE", "BENEFITS_ASSESSMENT_ELDERLY"), ("SOCIAL_SERVICES", "CHILD_FAMILY")),
    (("ADMINISTRATION", "LABOR_MARKET", None), ("SOCIAL_SERVICES",)),
]

# Moved from a TYPE-level node to a SUBTYPE-level node under an existing TYPE in the
# same category - needs the same move call as PLAIN_MOVES, but also a classification
# fix afterward (see fix_learning_center_classification()).
LEARNING_CENTER_MOVE = (("EDUCATION", "LEARNING_CENTER", None), ("EDUCATION", "ADULT_EDUCATION"))

# (source_path, dest_parent_path, new_resource_name, new_display_name)
MOVE_AND_RENAME = [
    (("URBAN_DEVELOPMENT", "PROPERTY", "MAINTENANCE"), ("URBAN_DEVELOPMENT", "PLANNING"), "ESTATES", "Fastigheter"),
    (("SOCIAL_SERVICES", "SUPPORT_CARE", "OCCUPATIONAL_THERAPIST"), ("SOCIAL_SERVICES", "MUNICIPAL_HEALTH_CARE"), "REHABILITATION_UNIT", "Rehabenheten"),
]

# Three old subtypes merged into the one new SPECIAL_HOUSING leaf added in ADDITIONS.
MERGE_SOURCES = [
    ("SOCIAL_SERVICES", "SPECIAL_HOUSING", "ELDERLY_DISABLED"),
    ("SOCIAL_SERVICES", "SPECIAL_HOUSING", "ELDERLY_DISABLED_SOL"),
    ("SOCIAL_SERVICES", "SPECIAL_HOUSING", "ELDERLY_ISSUES"),
]
MERGE_TARGET = ("SOCIAL_SERVICES", "ELDERLY_CARE", "SPECIAL_HOUSING")

# TYPE nodes to delete once emptied by the moves/merge above, checked for zero
# children at runtime before being touched - never deleted blindly.
CLEANUP_IF_EMPTY = [
    ("URBAN_DEVELOPMENT", "PROPERTY"),
    ("SOCIAL_SERVICES", "SPECIAL_HOUSING"),
]


# =============================================================================
# Label tree helpers - operate on the nested JSON structure the API returns
# (Labels.labelStructure: a list of root Label objects, each with its own
# nested "labels" list of children).
# =============================================================================

def _norm(path):
    """Drops the SUBTYPE slot's `None` (the ticket's `-`, meaning "this row is a TYPE, it
    has no subtype") so every other function only ever deals with the *effective* path -
    the node's own resourceName is always the last element, its parent is always
    everything before that, regardless of whether the row came from a 2-deep (category/type)
    or 3-deep (category/type/subtype) line in the ticket."""
    return tuple(p for p in path if p is not None)


def find(tree, path):
    """Walks `tree` (a list of root labels) by resourceName, returns the node dict or None."""
    nodes = tree
    node = None
    for name in _norm(path):
        node = next((n for n in nodes if n["resourceName"] == name), None)
        if node is None:
            return None
        nodes = node.setdefault("labels", [])
    return node


def require(tree, path, what):
    node = find(tree, path)
    if node is None:
        raise SystemExit(f"Could not find {what} at path {'/'.join(_norm(path))} - "
            "check the tree matches what the ticket assumes, or that an earlier step actually ran.")
    return node


def children_of(tree, path):
    path = _norm(path)
    if not path:
        return tree
    node = require(tree, path, "parent")
    return node.setdefault("labels", [])


def _nodes_at_depth(tree, depth, nodes=None, current_depth=1):
    """Every node anywhere in the tree at the given depth (1 = root/CATEGORY level)."""
    for node in (nodes if nodes is not None else tree):
        if current_depth == depth:
            yield node
        else:
            yield from _nodes_at_depth(tree, depth, node.get("labels", []), current_depth + 1)


def sibling_classification(tree, path):
    """The classification value used by whatever already sits at this depth, so a
    new node matches the convention already in use rather than guessing a casing.
    Looks at a direct sibling first; if there is none yet (e.g. the first child added
    to a TYPE that was itself only just added), falls back to any other node anywhere
    in the tree at the same depth, since the convention is a tree-wide one, not scoped
    to one parent."""
    path = _norm(path)
    siblings = children_of(tree, path[:-1])
    if siblings:
        return siblings[0]["classification"]

    anywhere = next(_nodes_at_depth(tree, len(path)), None)
    if anywhere is None:
        raise SystemExit(f"No existing label anywhere in the tree at depth {len(path)} to copy the "
            f"classification convention from for {'/'.join(path)} - add it with an explicit "
            "classification value instead of relying on this fallback.")
    return anywhere["classification"]


def add_node(tree, path, display_name):
    path = _norm(path)
    parent_path, resource_name = path[:-1], path[-1]
    siblings = children_of(tree, parent_path)
    if any(s["resourceName"] == resource_name for s in siblings):
        print(f"  (skip add, already present) {'/'.join(path)}")
        return
    siblings.append({
        "id": None,
        "classification": sibling_classification(tree, path),
        "displayName": display_name,
        "resourceName": resource_name,
        "labels": [],
    })
    print(f"  + add {'/'.join(path)} ({display_name!r})")


def rename_node(tree, path, new_display_name):
    node = require(tree, path, "label to rename")
    node["displayName"] = new_display_name
    print(f"  ~ rename {'/'.join(_norm(path))} -> {new_display_name!r}")


def delete_node(tree, path):
    path = _norm(path)
    parent_path, resource_name = path[:-1], path[-1]
    siblings = children_of(tree, parent_path)
    before = len(siblings)
    siblings[:] = [s for s in siblings if s["resourceName"] != resource_name]
    if len(siblings) == before:
        print(f"  (skip delete, not present) {'/'.join(p for p in path if p)}")
    else:
        print(f"  - delete {'/'.join(p for p in path if p)}")


# =============================================================================
# API client
# =============================================================================

@dataclass
class Client:
    base_url: str
    municipality_id: str
    namespace: str
    session: requests.Session = field(default_factory=requests.Session)

    @property
    def labels_url(self):
        return f"{self.base_url}/{self.municipality_id}/{self.namespace}/metadata/labels"

    def get_tree(self):
        response = self.session.get(self.labels_url)
        response.raise_for_status()
        return response.json()["labelStructure"]

    def put_tree(self, tree):
        response = self.session.put(self.labels_url, json=tree)
        response.raise_for_status()

    def move_dry_run(self, label_id, new_parent_id):
        return self._move_or_merge_call(f"{self.labels_url}/{label_id}/move",
            {"newParentId": new_parent_id, "dryRun": True})

    def start_move(self, label_id, new_parent_id):
        return self._move_or_merge_call(f"{self.labels_url}/{label_id}/move",
            {"newParentId": new_parent_id, "dryRun": False})

    def merge_dry_run(self, target_id, source_ids):
        return self._move_or_merge_call(f"{self.labels_url}/{target_id}/merge",
            {"sourceLabelIds": source_ids, "dryRun": True})

    def start_merge(self, target_id, source_ids):
        return self._move_or_merge_call(f"{self.labels_url}/{target_id}/merge",
            {"sourceLabelIds": source_ids, "dryRun": False})

    def _move_or_merge_call(self, url, body):
        response = self.session.post(url, json=body)
        response.raise_for_status()
        return response.json()

    def await_job(self, job_id, poll_seconds=2, timeout_seconds=300):
        url = f"{self.base_url}/{self.municipality_id}/{self.namespace}/jobs/{job_id}"
        deadline = time.time() + timeout_seconds
        while time.time() < deadline:
            response = self.session.get(url)
            response.raise_for_status()
            job = response.json()
            if job["status"] in ("COMPLETED", "FAILED", "STOPPED"):
                return job
            time.sleep(poll_seconds)
        raise SystemExit(f"Job {job_id} did not finish within {timeout_seconds}s")


def fetch_oauth2_token(token_url, client_id, client_secret):
    response = requests.post(token_url, data={"grant_type": "client_credentials"}, auth=(client_id, client_secret))
    response.raise_for_status()
    return response.json()["access_token"]


# =============================================================================
# Runbook steps
# =============================================================================

def run_tree_edit_step(client, tree, execute):
    print("\n=== Step 1: additions, renames, zero-errand deletions (one PUT) ===")
    for category, type_, subtype, display_name in ADDITIONS:
        add_node(tree, (category, type_, subtype), display_name)
    for category, type_, subtype, new_display_name in RENAMES:
        rename_node(tree, (category, type_, subtype), new_display_name)
    for category, type_, subtype in DELETIONS:
        delete_node(tree, (category, type_, subtype))

    if execute:
        client.put_tree(tree)
        print("  PUT sent.")
        tree = client.get_tree()
    else:
        print("  --execute not set: PUT not sent. Resulting tree would be:")
        print(json.dumps(tree, indent=2, ensure_ascii=False))
    return tree


def run_move(client, tree, source_path, dest_parent_path, label_desc, execute):
    label = require(tree, source_path, label_desc)
    new_parent = require(tree, dest_parent_path, f"destination parent for {label_desc}")

    dry = client.move_dry_run(label["id"], new_parent["id"])
    print(f"  {label_desc}: dry-run affects {dry['affectedErrandCount']} errand(s), "
        f"{len(dry.get('affectedActions', []))} action(s)")

    if not execute:
        return

    job = client.start_move(label["id"], new_parent["id"])
    print(f"  {label_desc}: move job {job['jobId']} started, waiting...")
    ended = client.await_job(job["jobId"])
    print(f"  {label_desc}: job ended {ended['status']} - {ended.get('message', '')}")
    if ended["status"] != "COMPLETED":
        raise SystemExit(f"Move of {label_desc} did not complete - stopping so nothing downstream runs against a half-moved tree.")


def run_plain_moves(client, tree, execute):
    print("\n=== Step 2: plain 1:1 moves ===")
    for source_path, dest_parent_path in PLAIN_MOVES:
        run_move(client, tree, source_path, dest_parent_path, "/".join(p for p in source_path if p), execute)

    source_path, dest_parent_path = LEARNING_CENTER_MOVE
    run_move(client, tree, source_path, dest_parent_path, "LEARNING_CENTER", execute)
    if execute:
        fix_learning_center_classification(client)


def fix_learning_center_classification(client):
    """LEARNING_CENTER moved from TYPE depth to SUBTYPE depth - the move endpoint does not
    touch `classification`, so it is corrected here with a follow-up tree PUT, copying
    whatever classification value ADULT_EDUCATION's other SUBTYPE children already use."""
    tree = client.get_tree()
    new_path = ("EDUCATION", "ADULT_EDUCATION", "LEARNING_CENTER")
    node = require(tree, new_path, "LEARNING_CENTER after its move")
    correct_classification = sibling_classification(tree, new_path)
    if node["classification"] != correct_classification:
        node["classification"] = correct_classification
        client.put_tree(tree)
        print(f"  LEARNING_CENTER classification corrected to {correct_classification!r}")


def run_move_and_rename(client, tree, execute):
    print("\n=== Step 3: moves that also change the code/display name ===")
    for source_path, dest_parent_path, new_resource_name, new_display_name in MOVE_AND_RENAME:
        label_desc = "/".join(p for p in source_path if p)
        run_move(client, tree, source_path, dest_parent_path, label_desc, execute)

        if not execute:
            continue

        renamed_tree = client.get_tree()
        new_path = dest_parent_path + (source_path[-1],)
        node = require(renamed_tree, new_path, f"{label_desc} after its move")
        node["resourceName"] = new_resource_name
        node["displayName"] = new_display_name
        client.put_tree(renamed_tree)
        print(f"  {label_desc}: renamed to {new_resource_name!r} / {new_display_name!r}")


def run_merge(client, tree, execute):
    print("\n=== Step 4: SPECIAL_HOUSING merge (3 old subtypes -> 1 new subtype) ===")
    target = require(tree, MERGE_TARGET, "merge target")
    source_ids = [require(tree, path, "/".join(path))["id"] for path in MERGE_SOURCES]

    dry = client.merge_dry_run(target["id"], source_ids)
    print(f"  dry-run affects {dry['affectedErrandCount']} errand(s), {len(dry.get('affectedActions', []))} action(s)")

    if not execute:
        return

    job = client.start_merge(target["id"], source_ids)
    print(f"  merge job {job['jobId']} started, waiting...")
    ended = client.await_job(job["jobId"])
    print(f"  job ended {ended['status']} - {ended.get('message', '')}")
    if ended["status"] != "COMPLETED":
        raise SystemExit("Merge did not complete - stopping before cleanup.")


def run_cleanup(client, execute):
    print("\n=== Step 5: delete now-empty old parents, if they are in fact empty ===")
    if not execute:
        print("  (skipped in dry-run mode)")
        return

    tree = client.get_tree()
    changed = False
    for path in CLEANUP_IF_EMPTY:
        node = find(tree, path)
        if node is None:
            continue
        if node.get("labels"):
            print(f"  {'/'.join(path)}: still has children, leaving it")
            continue
        delete_node(tree, path)
        changed = True

    if changed:
        client.put_tree(tree)
        print("  PUT sent.")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base-url", required=True, help="e.g. https://api-support-management.<env>.sundsvall.se")
    parser.add_argument("--municipality-id", required=True, help="KC Ånge's municipalityId")
    parser.add_argument("--namespace", required=True, help="KC Ånge's namespace")
    parser.add_argument("--token-url", help="OAuth2 client-credentials token endpoint")
    parser.add_argument("--client-id")
    parser.add_argument("--client-secret")
    parser.add_argument("--token", help="Use a pre-fetched bearer token instead of --token-url/--client-id/--client-secret")
    parser.add_argument("--execute", action="store_true", help="Actually write. Without this, only dry-runs and prints are performed.")
    args = parser.parse_args()

    if args.token:
        token = args.token
    elif args.token_url and args.client_id and args.client_secret:
        token = fetch_oauth2_token(args.token_url, args.client_id, args.client_secret)
    else:
        sys.exit("Provide either --token, or all of --token-url/--client-id/--client-secret")

    client = Client(base_url=args.base_url.rstrip("/"), municipality_id=args.municipality_id, namespace=args.namespace)
    client.session.headers["Authorization"] = f"Bearer {token}"
    client.session.headers["Content-Type"] = "application/json"

    if not args.execute:
        print("*** DRY RUN (pass --execute to actually write) ***")

    tree = client.get_tree()
    tree = run_tree_edit_step(client, tree, args.execute)
    run_plain_moves(client, tree, args.execute)
    run_move_and_rename(client, tree, args.execute)

    tree = client.get_tree() if args.execute else tree
    run_merge(client, tree, args.execute)

    run_cleanup(client, args.execute)

    print("\nDone.")


if __name__ == "__main__":
    main()
