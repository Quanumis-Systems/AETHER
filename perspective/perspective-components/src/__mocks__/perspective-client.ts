// Runtime Mock of the IA Perspective SDK for standalone testing & execution
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

export const Component = function() {
    return function(target: any) {
        return target;
    }
};

export const ComponentRegistry = {
    register: (id: string, comp: any) => console.log(`[ComponentRegistry] Mock Registered Component: ${id}`)
};
