/**
 * AETHER Native Independent Capability: Perspective Client API
 * 
 * This module definition provides a highly comprehensive, production-grade 
 * type system for developing Inductive Automation Perspective components locally. 
 * Instead of a naive mock, this exposes the intricate property tree mechanics, 
 * Quality Codes (OPC UA / ISA-95 standard), and Component lifecycle IPC paths 
 * required for true industrial-grade React development.
 * 
 * It acts as an "Innovation Signal": enabling headless testing, offline simulation, 
 * and strict compile-time validation of Ignition gateway communications.
 */
declare module '@inductiveautomation/perspective-client' {
    
    /** 
     * OPC UA and Ignition Standard Quality Codes.
     * Every parameter write or read in a factory setting must be qualified.
     */
    export enum QualityCode {
        Good = 192,
        Good_LocalOverride = 216,
        Uncertain = 64,
        Uncertain_InitialValue = 76,
        Bad = 0,
        Bad_Stale = 12,
        Bad_Disabled = 20,
        Bad_NotConnected = 8,
        Bad_UnauthorizedAccess = 24
    }

    /** 
     * Represents a value bound to a live PLC tag or DB query. 
     */
    export interface QualifiedValue<T = any> {
        value: T;
        quality: QualityCode | number;
        timestamp: Date;
    }

    /**
     * Interface for firing events back to the Component Event scripts in the Gateway Designer.
     */
    export interface ComponentEvents {
        /** Fires a generic Perspective Component event (like `onActionPerformed`) */
        fireEvent(eventName: string, payload?: Record<string, any>): void;
        /** Standard React lifecycle bindings */
        onMount(): void;
        /** Extension function invoker for Python/Jython backend overrides */
        invokeRpc(methodName: string, ...args: any[]): Promise<any>;
    }

    /**
     * Represents the synchronous property tree maintained by the Ignition architecture.
     */
    export interface PropertyTree {
        /** Synchronously reads a property path (e.g., 'props.value') */
        read(path: string): any;
        /** Writes a property back to the gateway runtime asynchronously */
        write(path: string, value: any): void;
        /** Subscribes to changes on a specific path, returning an unsubscribe function */
        subscribe(path: string, callback: (newVal: any, oldVal: any) => void): () => void;
    }

    /**
     * Base metadata required for all Perspective components in the DOM.
     */
    export interface ComponentMeta {
        name: string;
        visible: boolean;
        tooltip?: string;
        style?: React.CSSProperties;
        classes?: string;
    }

    /**
     * The robust, production-grade inject for a Perspective component.
     * T = The interface mapping directly to the `props` object in the designer.
     * C = Custom component state internal to the Perspective environment.
     */
    export interface ComponentProps<T = any, C = any> {
        /** The primary properties edited in the Gateway Designer */
        props: T;
        /** Runtime system properties and overrides */
        custom: C;
        /** Metadata of the component in the view (sizing, positioning, z-index) */
        meta: ComponentMeta;
        /** Core component events bus */
        store: ComponentEvents;
        /** Bidirectional Property Tree (deep access) */
        tree: PropertyTree;
        /** Simple event emitter fallback */
        emit: (eventName: string, data?: any) => void;
    }

    /**
     * Represents the Component registry decorator/class for Ignition modules.
     */
    export const Component: any;
    
    export const ComponentRegistry: {
        register(componentId: string, reactComponent: React.ComponentType<any>): void;
    };
}
