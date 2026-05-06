package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.ops.entity.SfDeliveryRecord;

import java.util.List;

public interface DeliveryService extends IService<SfDeliveryRecord> {

    /**
     * Create a delivery record for an artifact.
     *
     * @param artifactId   the artifact ID
     * @param deliveryType the delivery type
     * @param recipient    the recipient
     * @return the created delivery record
     */
    SfDeliveryRecord createDelivery(Long artifactId, String deliveryType, String recipient);

    /**
     * Track delivery status.
     *
     * @param deliveryId the delivery ID
     * @param status     the new status
     * @return the updated delivery record
     */
    SfDeliveryRecord trackDelivery(Long deliveryId, Integer status);

    /**
     * Confirm a delivery as completed.
     *
     * @param deliveryId the delivery ID
     * @return the confirmed delivery record
     */
    SfDeliveryRecord confirmDelivery(Long deliveryId);

    /**
     * List delivery records by status.
     *
     * @param status the status code
     * @return list of delivery records
     */
    List<SfDeliveryRecord> listByStatus(Integer status);
}
