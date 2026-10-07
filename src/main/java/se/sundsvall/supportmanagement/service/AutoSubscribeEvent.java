package se.sundsvall.supportmanagement.service;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

/**
 * Published when something happened on an errand, so that the people tied to it may be subscribed to it once the
 * change is committed. The reporter is only subscribed as the errand is created, so that leaving the subscription is
 * not undone by the next change to the errand.
 *
 * @param errandEntity  the errand
 * @param errandCreated whether the event is the errand being created
 */
record AutoSubscribeEvent(ErrandEntity errandEntity, boolean errandCreated) {

	AutoSubscribeEvent(final ErrandEntity errandEntity) {
		this(errandEntity, false);
	}
}
