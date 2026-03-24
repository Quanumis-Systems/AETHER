import React, { useState, useEffect, useCallback, useRef } from 'react';
import { ComponentProps, QualityCode } from '@inductiveautomation/perspective-client';
import './TblOrchestrator.css';

interface TblOrchestratorProps {
    profitWeight: number;
    peopleWeight: number;
    planetWeight: number;
    onWeightRebalance: (w1: number, w2: number, w3: number) => void;
    recentAgentRecommendation: string;
    quality?: QualityCode;
    isEnabled?: boolean;
}

function useRpcDebounce<T extends (...args: any[]) => void>(callback: T, delay: number) {
    const timeoutRef = useRef<NodeJS.Timeout | null>(null);
    return useCallback((...args: Parameters<T>) => {
        if (timeoutRef.current) clearTimeout(timeoutRef.current);
        timeoutRef.current = setTimeout(() => callback(...args), delay);
    }, [callback, delay]);
}

const TblOrchestratorRender: React.FC<ComponentProps<TblOrchestratorProps>> = (props) => {
    
    const [weights, setWeights] = useState({
        profit: props.props.profitWeight ?? 0.33,
        people: props.props.peopleWeight ?? 0.33,
        planet: props.props.planetWeight ?? 0.34
    });

    const [isSyncing, setIsSyncing] = useState(false);
    const [chatHistory, setChatHistory] = useState<{sender: string, text: string}[]>([
        { sender: 'agent', text: "AETHER RAG Engine Matrix Online. Validating GRI/SASB compliance standards..." },
        { sender: 'agent', text: props.props.recentAgentRecommendation || "Evaluating local utility rate structures. Awaiting parameter shift." }
    ]);
    const [chatInput, setChatInput] = useState('');

    useEffect(() => {
        setWeights({
            profit: props.props.profitWeight ?? 0.33,
            people: props.props.peopleWeight ?? 0.33,
            planet: props.props.planetWeight ?? 0.34
        });
        setIsSyncing(false);
    }, [props.props.profitWeight, props.props.peopleWeight, props.props.planetWeight]);

    const rebalanceRpc = useRpcDebounce((w1: number, w2: number, w3: number) => {
        if (props.props.onWeightRebalance) {
            props.props.onWeightRebalance(w1, w2, w3);
        } else if (props.emit) {
            props.emit('onWeightRebalance', { w1, w2, w3 });
        }
    }, 400);

    const handleSliderChange = (type: 'profit' | 'people' | 'planet', value: number) => {
        setIsSyncing(true);
        const newWeights = { ...weights, [type]: value };
        setWeights(newWeights);
        rebalanceRpc(newWeights.profit, newWeights.people, newWeights.planet);
    };

    const handleChatSubmit = () => {
        if (!chatInput.trim()) return;
        setChatHistory(prev => [...prev, { sender: 'user', text: chatInput }]);
        
        // Mock Agentic Reply referencing the vector space
        setTimeout(() => {
            setChatHistory(prev => [...prev, { 
                sender: 'agent', 
                text: "Based on the embedded QuestDB telemetry and SASB Standard RT-CH-110a.1, I constrained line speed by 5% because the ONNX prediction for the next 15m exceeded the CI budget set by the Planet Priority." 
            }]);
        }, 1200);
        
        setChatInput('');
    };

    // Calculate dynamic Pareto point based on sliders: x = Planet/People Utility, y = Profit
    // This provides visual intuition into the "Sweet Spot".
    const paretoX = 15 + ((weights.planet + weights.people) / 2) * 80; // 15% to 95%
    const paretoY = 15 + weights.profit * 70; // 15% to 85%

    const qCode = props.props.quality ?? QualityCode.Good;
    const isQualityGood = qCode === QualityCode.Good || qCode === QualityCode.Good_LocalOverride;
    const isDisabled = props.props.isEnabled === false || qCode === QualityCode.Bad_NotConnected;

    return (
        <div className="tbl-dashboard-layout">
            <div className={`tbl-container ${isDisabled ? 'tbl-disabled' : ''}`}>
                <div style={{borderBottom: '2px solid #333', paddingBottom: '16px', marginBottom: '24px'}}>
                    <h2 style={{margin: '0 0 8px 0', fontWeight: 600}}>AETHER TBL Orchestrator</h2>
                    <p style={{margin: 0, color: '#a0a0a0'}}>Industrial Semantic Trajectory Engine Interface</p>
                </div>
                
                <SliderGroup type="profit" label="Financial ROI Setpoint" value={weights.profit} disabled={isDisabled} onChange={handleSliderChange} />
                <SliderGroup type="people" label="Safety/Cognitive Setpoint" value={weights.people} disabled={isDisabled} onChange={handleSliderChange} />
                <SliderGroup type="planet" label="ESG/Emissions Setpoint" value={weights.planet} disabled={isDisabled} onChange={handleSliderChange} />

                {/* Pareto Front Visualizer */}
                <div className="pareto-vis">
                    <div className="pareto-title">Pareto Efficiency Frontier</div>
                    <div className="pareto-axis-x"></div>
                    <div className="pareto-axis-y"></div>
                    <div className="pareto-label-x">Sustainability & Safety →</div>
                    <div className="pareto-label-y">Yield ↑</div>
                    
                    {/* The dynamic "Sweet Spot" marker projected onto the simulated frontier */}
                    <div className="pareto-point" style={{ left: `${paretoX}%`, bottom: `${paretoY}%` }} title="Agentic Convergence Point"></div>
                </div>

                <div style={{display: 'flex', justifyContent: 'flex-end', marginTop: 20}}>
                    <button 
                        style={{background: '#4CAF50', color: 'white', border: 'none', padding: '12px 24px', borderRadius: '6px', fontWeight: 600, cursor: isQualityGood ? 'pointer' : 'not-allowed', opacity: isQualityGood ? 1 : 0.5}}
                        disabled={isDisabled || !isQualityGood}
                    >
                        {isSyncing ? "Evaluating..." : "Authorize PLC Sequence"}
                    </button>
                </div>
            </div>

            {/* Sub-system B: Agentic ESG RAG Interface */}
            <div className="tbl-sidebar-chat">
                <div className="chat-header">
                    <h3>Boardroom Agent Copilot</h3>
                </div>
                <div className="chat-history">
                    {chatHistory.map((msg, i) => (
                        <div key={i} className={`chat-msg msg-${msg.sender}`}>
                            {msg.text}
                        </div>
                    ))}
                </div>
                <div className="chat-input-area">
                    <input 
                        type="text" 
                        placeholder="E.g. Why did you reduce speed?"
                        value={chatInput}
                        onChange={e => setChatInput(e.target.value)}
                        onKeyPress={e => e.key === 'Enter' && handleChatSubmit()}
                    />
                    <button onClick={handleChatSubmit}>Send</button>
                </div>
            </div>
        </div>
    );
};

const SliderGroup = ({ type, label, value, disabled, onChange }: any) => (
    <div style={{background: '#242424', padding: '16px', borderRadius: '8px', marginBottom: '16px', borderLeft: `4px solid ${type === 'profit' ? '#4CAF50' : type === 'people' ? '#2196F3' : '#9C27B0'}`}}>
        <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: '12px'}}>
            <span style={{fontWeight: 500, display: 'flex', gap: 8}}>{label}</span>
            <span style={{fontWeight: 'bold', background: '#111', padding: '2px 8px', borderRadius: 4}}>{value.toFixed(2)}</span>
        </div>
        <input type="range" style={{width: '100%'}} min="0" max="1" step="0.01" value={value} disabled={disabled} onChange={(e) => onChange(type, parseFloat(e.target.value))} />
    </div>
);

export const TblOrchestrator = (props: ComponentProps<TblOrchestratorProps>) => <TblOrchestratorRender {...props} />;
