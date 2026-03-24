import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { TblOrchestrator } from './TblOrchestrator';
import { QualityCode } from '@inductiveautomation/perspective-client';

describe('TblOrchestrator Component', () => {
    const defaultProps = {
        props: {
            profitWeight: 0.50,
            peopleWeight: 0.30,
            planetWeight: 0.20,
            onWeightRebalance: jest.fn(),
            recentAgentRecommendation: 'Test Recommendation',
            quality: QualityCode.Good,
            isEnabled: true
        },
        custom: {},
        meta: { name: 'test', visible: true },
        store: {
            fireEvent: jest.fn(),
            onMount: jest.fn(),
            invokeRpc: jest.fn()
        },
        tree: { read: jest.fn(), write: jest.fn(), subscribe: jest.fn() },
        emit: jest.fn()
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('renders with initial weights', () => {
        render(<TblOrchestrator {...defaultProps} />);
        expect(screen.getByText(/Financial ROI Setpoint/i)).not.toBeNull();
        expect(screen.getByText('0.50')).not.toBeNull();
        expect(screen.getByText('0.30')).not.toBeNull();
        expect(screen.getByText('0.20')).not.toBeNull();
        expect(screen.getByText('Test Recommendation')).not.toBeNull();
    });

    test('shows visual quality code indicators', () => {
        const propsWithBadQuality = {
            ...defaultProps,
            props: { ...defaultProps.props, quality: QualityCode.Bad_Stale }
        };
        render(<TblOrchestrator {...propsWithBadQuality} />);
        
        // Quality indicators should say Bad/Stale
        const badBadges = screen.getAllByText('Bad/Stale');
        expect(badBadges.length).toBe(3); // one for each slider
        
        // Authorization button should be disabled
        const authBtn = screen.getByRole('button', { name: /Authorize/i });
        expect(authBtn.hasAttribute('disabled')).toBe(true);
    });

    test('invokes RPC authorizeSequence on button click', () => {
        render(<TblOrchestrator {...defaultProps} />);
        const authBtn = screen.getByRole('button', { name: /Authorize PLC Sequence/i });
        fireEvent.click(authBtn);
        
        expect(defaultProps.store.invokeRpc).toHaveBeenCalledWith('authorizeSequence');
    });
});
