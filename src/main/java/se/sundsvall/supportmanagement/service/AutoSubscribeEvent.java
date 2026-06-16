package se.sundsvall.supportmanagement.service;

import se.sundsvall.supportmanagement.integration.db.model.ErrandEntity;

record AutoSubscribeEvent(ErrandEntity errandEntity) {}
