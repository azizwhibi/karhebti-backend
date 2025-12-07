/**
 * Routes pour la gestion des pannes (breakdowns)
 * Endpoints pour Accept/Refuse SOS requests
 */

const express = require('express');
const router = express.Router();
const { authenticateToken } = require('../middleware/auth');
const Breakdown = require('../models/Breakdown'); // Ajustez selon votre modèle

/**
 * PUT /api/breakdowns/:id/accept
 * Accepter une demande SOS (Garage Owner)
 */
router.put('/:id/accept', authenticateToken, async (req, res) => {
    try {
        const breakdownId = req.params.id;
        const garageOwnerId = req.user.sub; // ID depuis JWT
        const garageEmail = req.user.email;

        console.log(`🟢 [ACCEPT] Breakdown: ${breakdownId} by ${garageEmail}`);

        // Trouver le breakdown
        const breakdown = await Breakdown.findById(breakdownId);

        if (!breakdown) {
            console.log(`❌ Breakdown not found: ${breakdownId}`);
            return res.status(404).json({
                error: 'Breakdown not found',
                breakdownId
            });
        }

        // Vérifier le statut
        if (breakdown.status !== 'PENDING') {
            console.log(`⚠️ Breakdown already handled: ${breakdown.status}`);
            return res.status(400).json({
                error: 'Breakdown already handled',
                currentStatus: breakdown.status
            });
        }

        // Mettre à jour
        breakdown.status = 'ACCEPTED';
        breakdown.assignedTo = garageOwnerId;
        breakdown.acceptedAt = new Date();
        breakdown.updatedAt = new Date();

        await breakdown.save();

        console.log(`✅ Breakdown accepted: ${breakdownId} → Status: ACCEPTED`);

        // TODO: Envoyer notification au client
        // await notifyClient(breakdown.userId, {
        //     type: 'SOS_ACCEPTED',
        //     breakdownId: breakdown._id,
        //     garage: garageEmail
        // });

        // Retourner le breakdown complet avec tous les champs
        const response = {
            _id: breakdown._id.toString(),
            user_id: breakdown.userId,
            vehicle_id: breakdown.vehicleId,
            type: breakdown.type,
            status: breakdown.status,
            description: breakdown.description,
            latitude: breakdown.latitude,
            longitude: breakdown.longitude,
            assigned_to: breakdown.assignedTo,
            created_at: breakdown.createdAt,
            updated_at: breakdown.updatedAt,
            acceptedAt: breakdown.acceptedAt
        };

        res.json(response);

    } catch (error) {
        console.error('❌ Error accepting breakdown:', error);
        res.status(500).json({
            error: 'Internal server error',
            message: error.message
        });
    }
});

/**
 * PUT /api/breakdowns/:id/refuse
 * Refuser une demande SOS (Garage Owner)
 */
router.put('/:id/refuse', authenticateToken, async (req, res) => {
    try {
        const breakdownId = req.params.id;
        const garageOwnerId = req.user.sub;
        const garageEmail = req.user.email;
        const reason = req.body?.reason || 'No reason provided';

        console.log(`🔴 [REFUSE] Breakdown: ${breakdownId} by ${garageEmail}`);
        console.log(`   Reason: ${reason}`);

        const breakdown = await Breakdown.findById(breakdownId);

        if (!breakdown) {
            console.log(`❌ Breakdown not found: ${breakdownId}`);
            return res.status(404).json({
                error: 'Breakdown not found',
                breakdownId
            });
        }

        if (breakdown.status !== 'PENDING') {
            console.log(`⚠️ Breakdown already handled: ${breakdown.status}`);
            return res.status(400).json({
                error: 'Breakdown already handled',
                currentStatus: breakdown.status
            });
        }

        // Mettre à jour
        breakdown.status = 'REFUSED';
        breakdown.refusedBy = garageOwnerId;
        breakdown.refusedAt = new Date();
        breakdown.refusalReason = reason;
        breakdown.updatedAt = new Date();

        await breakdown.save();

        console.log(`ℹ️ Breakdown refused: ${breakdownId} → Status: REFUSED`);

        // TODO: Notifier le client et chercher un autre garage
        // await notifyClient(breakdown.userId, {
        //     type: 'SOS_REFUSED',
        //     breakdownId: breakdown._id
        // });
        // await findAlternativeGarage(breakdown);

        res.json({
            message: 'Breakdown refused',
            breakdownId: breakdown._id.toString(),
            status: breakdown.status
        });

    } catch (error) {
        console.error('❌ Error refusing breakdown:', error);
        res.status(500).json({
            error: 'Internal server error',
            message: error.message
        });
    }
});

/**
 * GET /api/breakdowns/:id
 * Récupérer les détails d'un breakdown (support String ID)
 */
router.get('/:id', authenticateToken, async (req, res) => {
    try {
        const breakdownId = req.params.id;

        console.log(`🔍 [GET] Breakdown: ${breakdownId}`);

        const breakdown = await Breakdown.findById(breakdownId)
            .populate('userId', 'nom prenom email telephone');

        if (!breakdown) {
            console.log(`❌ Breakdown not found: ${breakdownId}`);
            return res.status(404).json({
                error: 'Breakdown not found',
                breakdownId
            });
        }

        const response = {
            _id: breakdown._id.toString(),
            user_id: breakdown.userId,
            vehicle_id: breakdown.vehicleId,
            type: breakdown.type,
            status: breakdown.status,
            description: breakdown.description,
            latitude: breakdown.latitude,
            longitude: breakdown.longitude,
            assigned_to: breakdown.assignedTo,
            created_at: breakdown.createdAt,
            updated_at: breakdown.updatedAt
        };

        res.json(response);

    } catch (error) {
        console.error('❌ Error getting breakdown:', error);
        res.status(500).json({
            error: 'Internal server error',
            message: error.message
        });
    }
});

/**
 * GET /api/breakdowns
 * Liste des breakdowns avec filtres (status, userId)
 */
router.get('/', authenticateToken, async (req, res) => {
    try {
        const { status, userId } = req.query;
        const query = {};

        if (status) {
            query.status = status.toUpperCase();
        }

        if (userId) {
            query.userId = userId;
        }

        console.log(`📋 [LIST] Breakdowns - Query:`, query);

        const breakdowns = await Breakdown.find(query)
            .populate('userId', 'nom prenom email telephone')
            .sort({ createdAt: -1 });

        // Formater la réponse pour Android
        const formattedBreakdowns = breakdowns.map(b => ({
            _id: b._id.toString(),
            user_id: b.userId?._id?.toString(),
            vehicle_id: b.vehicleId,
            type: b.type,
            status: b.status,
            description: b.description,
            latitude: b.latitude,
            longitude: b.longitude,
            assigned_to: b.assignedTo,
            created_at: b.createdAt,
            updated_at: b.updatedAt
        }));

        console.log(`✅ Found ${formattedBreakdowns.length} breakdowns`);

        // IMPORTANT: Retourner directement le tableau, PAS un objet wrapper
        res.json(formattedBreakdowns);

    } catch (error) {
        console.error('❌ Error listing breakdowns:', error);
        res.status(500).json({
            error: 'Internal server error',
            message: error.message
        });
    }
});

module.exports = router;

