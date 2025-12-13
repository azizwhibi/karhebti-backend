/**
 * Backend API Endpoints for SOS Breakdown Accept/Refuse
 *
 * Add these endpoints to your backend Express.js routes
 * File: backend/routes/breakdowns.js
 */

const express = require('express');
const router = express.Router();
const { authenticateToken } = require('../middleware/auth');
const { Breakdown, User, Garage, Device } = require('../models');
const { sendFCMNotification } = require('../services/notificationService');

/**
 * PUT /api/breakdowns/:id/accept
 *
 * Garage owner accepts a breakdown request
 *
 * Authentication: Required (JWT)
 * Role: garage_owner
 *
 * Response:
 * - 200: Success, breakdown accepted
 * - 403: Not authorized (not garage owner or not verified)
 * - 404: Breakdown not found
 * - 409: Breakdown already accepted by another garage
 */
router.put('/:id/accept', authenticateToken, async (req, res) => {
    try {
        const breakdownId = req.params.id;
        const garageOwnerId = req.user.id;

        console.log(`📥 PUT /api/breakdowns/${breakdownId}/accept`);
        console.log(`   User: ${req.user.email} (ID: ${garageOwnerId})`);

        // Verify user is garage owner
        const user = await User.findByPk(garageOwnerId);
        if (!user || user.role !== 'garage_owner') {
            console.log(`❌ User is not a garage owner`);
            return res.status(403).json({
                error: 'Only garage owners can accept breakdown requests'
            });
        }

        // Get garage
        const garage = await Garage.findOne({
            where: { userId: garageOwnerId }
        });

        if (!garage) {
            console.log(`❌ No garage found for user`);
            return res.status(403).json({
                error: 'No garage associated with this account'
            });
        }

        if (!garage.verified) {
            console.log(`❌ Garage not verified`);
            return res.status(403).json({
                error: 'Your garage must be verified to accept requests'
            });
        }

        // Find breakdown
        const breakdown = await Breakdown.findByPk(breakdownId, {
            include: [{ model: User, as: 'user' }]
        });

        if (!breakdown) {
            console.log(`❌ Breakdown not found`);
            return res.status(404).json({
                error: 'Breakdown not found'
            });
        }

        // Check if already accepted
        if (breakdown.status === 'ACCEPTED') {
            console.log(`⚠️ Breakdown already accepted by another garage`);
            return res.status(409).json({
                error: 'This breakdown has already been accepted by another garage',
                acceptedBy: breakdown.acceptedBy
            });
        }

        // Check if cancelled
        if (breakdown.status === 'CANCELLED' || breakdown.status === 'COMPLETED') {
            console.log(`⚠️ Breakdown is ${breakdown.status}`);
            return res.status(409).json({
                error: `This breakdown is already ${breakdown.status.toLowerCase()}`
            });
        }

        // Update breakdown
        await breakdown.update({
            status: 'ACCEPTED',
            acceptedBy: garage.id,
            acceptedAt: new Date()
        });

        console.log(`✅ Breakdown accepted successfully`);
        console.log(`   Breakdown ID: ${breakdownId}`);
        console.log(`   Garage: ${garage.name} (ID: ${garage.id})`);
        console.log(`   Status: PENDING → ACCEPTED`);

        // Send notification to user (client)
        try {
            const clientDevices = await Device.findAll({
                where: { userId: breakdown.userId }
            });

            if (clientDevices.length > 0) {
                console.log(`📤 Sending acceptance notification to client...`);

                for (const device of clientDevices) {
                    if (device.fcmToken) {
                        await sendFCMNotification({
                            token: device.fcmToken,
                            title: '✅ Garage trouvé!',
                            body: `${garage.name} a accepté votre demande SOS`,
                            data: {
                                type: 'BREAKDOWN_ACCEPTED',
                                breakdownId: breakdownId.toString(),
                                garageId: garage.id.toString(),
                                garageName: garage.name
                            }
                        });
                    }
                }

                console.log(`✅ Acceptance notification sent to client`);
            }
        } catch (notifError) {
            console.error(`⚠️ Failed to send notification to client: ${notifError.message}`);
            // Don't fail the request if notification fails
        }

        // Return updated breakdown
        const updatedBreakdown = await Breakdown.findByPk(breakdownId, {
            include: [
                { model: User, as: 'user' },
                { model: Garage, as: 'acceptedByGarage' }
            ]
        });

        res.json({
            message: 'Breakdown accepted successfully',
            breakdown: updatedBreakdown
        });

    } catch (error) {
        console.error(`❌ Error accepting breakdown: ${error.message}`, error);
        res.status(500).json({
            error: 'Failed to accept breakdown',
            details: error.message
        });
    }
});

/**
 * PUT /api/breakdowns/:id/refuse
 *
 * Garage owner refuses a breakdown request
 *
 * Authentication: Required (JWT)
 * Role: garage_owner
 *
 * Request Body (optional):
 * {
 *   "reason": "Too far / Already busy / Other"
 * }
 *
 * Response:
 * - 200: Success, breakdown refused
 * - 403: Not authorized
 * - 404: Breakdown not found
 */
router.put('/:id/refuse', authenticateToken, async (req, res) => {
    try {
        const breakdownId = req.params.id;
        const garageOwnerId = req.user.id;
        const { reason } = req.body;

        console.log(`📥 PUT /api/breakdowns/${breakdownId}/refuse`);
        console.log(`   User: ${req.user.email} (ID: ${garageOwnerId})`);
        if (reason) console.log(`   Reason: ${reason}`);

        // Verify user is garage owner
        const user = await User.findByPk(garageOwnerId);
        if (!user || user.role !== 'garage_owner') {
            console.log(`❌ User is not a garage owner`);
            return res.status(403).json({
                error: 'Only garage owners can refuse breakdown requests'
            });
        }

        // Get garage
        const garage = await Garage.findOne({
            where: { userId: garageOwnerId }
        });

        if (!garage) {
            console.log(`❌ No garage found for user`);
            return res.status(403).json({
                error: 'No garage associated with this account'
            });
        }

        // Find breakdown
        const breakdown = await Breakdown.findByPk(breakdownId);

        if (!breakdown) {
            console.log(`❌ Breakdown not found`);
            return res.status(404).json({
                error: 'Breakdown not found'
            });
        }

        // Log the refusal
        console.log(`ℹ️ Garage ${garage.name} refused breakdown ${breakdownId}`);

        // Optionally store refusal in a separate table for analytics
        // await BreakdownRefusal.create({
        //     breakdownId,
        //     garageId: garage.id,
        //     reason
        // });

        // Find other nearby garages to notify
        // This is optional - you could re-trigger the notification logic here

        res.json({
            message: 'Breakdown refused',
            breakdownId: breakdownId
        });

    } catch (error) {
        console.error(`❌ Error refusing breakdown: ${error.message}`, error);
        res.status(500).json({
            error: 'Failed to refuse breakdown',
            details: error.message
        });
    }
});

/**
 * PUT /api/breakdowns/:id/cancel
 *
 * Client cancels their breakdown request
 *
 * Authentication: Required (JWT)
 * Role: client (breakdown owner)
 *
 * Response:
 * - 200: Success, breakdown cancelled
 * - 403: Not authorized (not breakdown owner)
 * - 404: Breakdown not found
 * - 409: Cannot cancel (already completed)
 */
router.put('/:id/cancel', authenticateToken, async (req, res) => {
    try {
        const breakdownId = req.params.id;
        const userId = req.user.id;

        console.log(`📥 PUT /api/breakdowns/${breakdownId}/cancel`);
        console.log(`   User: ${req.user.email} (ID: ${userId})`);

        // Find breakdown
        const breakdown = await Breakdown.findByPk(breakdownId);

        if (!breakdown) {
            console.log(`❌ Breakdown not found`);
            return res.status(404).json({
                error: 'Breakdown not found'
            });
        }

        // Verify ownership
        if (breakdown.userId !== userId) {
            console.log(`❌ User does not own this breakdown`);
            return res.status(403).json({
                error: 'You can only cancel your own breakdown requests'
            });
        }

        // Check if can be cancelled
        if (breakdown.status === 'COMPLETED') {
            console.log(`⚠️ Cannot cancel completed breakdown`);
            return res.status(409).json({
                error: 'Cannot cancel a completed breakdown'
            });
        }

        // Update status
        await breakdown.update({
            status: 'CANCELLED',
            cancelledAt: new Date()
        });

        console.log(`✅ Breakdown cancelled successfully`);

        // Notify garage if it was accepted
        if (breakdown.acceptedBy) {
            try {
                const garage = await Garage.findByPk(breakdown.acceptedBy, {
                    include: [{ model: User }]
                });

                if (garage && garage.User) {
                    const garageDevices = await Device.findAll({
                        where: { userId: garage.User.id }
                    });

                    for (const device of garageDevices) {
                        if (device.fcmToken) {
                            await sendFCMNotification({
                                token: device.fcmToken,
                                title: '❌ Demande annulée',
                                body: 'Le client a annulé sa demande SOS',
                                data: {
                                    type: 'BREAKDOWN_CANCELLED',
                                    breakdownId: breakdownId.toString()
                                }
                            });
                        }
                    }

                    console.log(`✅ Cancellation notification sent to garage`);
                }
            } catch (notifError) {
                console.error(`⚠️ Failed to notify garage: ${notifError.message}`);
            }
        }

        res.json({
            message: 'Breakdown cancelled successfully',
            breakdown
        });

    } catch (error) {
        console.error(`❌ Error cancelling breakdown: ${error.message}`, error);
        res.status(500).json({
            error: 'Failed to cancel breakdown',
            details: error.message
        });
    }
});

/**
 * PUT /api/breakdowns/:id/complete
 *
 * Garage marks breakdown as completed
 *
 * Authentication: Required (JWT)
 * Role: garage_owner (must be the one who accepted)
 *
 * Response:
 * - 200: Success, breakdown marked as completed
 * - 403: Not authorized
 * - 404: Breakdown not found
 */
router.put('/:id/complete', authenticateToken, async (req, res) => {
    try {
        const breakdownId = req.params.id;
        const garageOwnerId = req.user.id;

        console.log(`📥 PUT /api/breakdowns/${breakdownId}/complete`);
        console.log(`   User: ${req.user.email} (ID: ${garageOwnerId})`);

        // Get garage
        const garage = await Garage.findOne({
            where: { userId: garageOwnerId }
        });

        if (!garage) {
            return res.status(403).json({
                error: 'No garage associated with this account'
            });
        }

        // Find breakdown
        const breakdown = await Breakdown.findByPk(breakdownId);

        if (!breakdown) {
            return res.status(404).json({
                error: 'Breakdown not found'
            });
        }

        // Verify this garage accepted the breakdown
        if (breakdown.acceptedBy !== garage.id) {
            return res.status(403).json({
                error: 'Only the garage that accepted this breakdown can mark it as completed'
            });
        }

        // Update status
        await breakdown.update({
            status: 'COMPLETED',
            completedAt: new Date()
        });

        console.log(`✅ Breakdown marked as completed`);

        // Notify client
        try {
            const clientDevices = await Device.findAll({
                where: { userId: breakdown.userId }
            });

            for (const device of clientDevices) {
                if (device.fcmToken) {
                    await sendFCMNotification({
                        token: device.fcmToken,
                        title: '✅ Service terminé',
                        body: `${garage.name} a terminé la réparation`,
                        data: {
                            type: 'BREAKDOWN_COMPLETED',
                            breakdownId: breakdownId.toString()
                        }
                    });
                }
            }

            console.log(`✅ Completion notification sent to client`);
        } catch (notifError) {
            console.error(`⚠️ Failed to notify client: ${notifError.message}`);
        }

        res.json({
            message: 'Breakdown marked as completed',
            breakdown
        });

    } catch (error) {
        console.error(`❌ Error completing breakdown: ${error.message}`, error);
        res.status(500).json({
            error: 'Failed to complete breakdown',
            details: error.message
        });
    }
});

module.exports = router;

