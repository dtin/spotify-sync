import { useState } from "react";

export function useSelection(items: any[], idField: string) {
    const [selectedIds, setSelectedIds] = useState<string[]>([]);

    const isAllSelected = items.length > 0 && selectedIds.length === items.length;
    
    // Check if at least one but not all are selected
    const isIndeterminate = selectedIds.length > 0 && selectedIds.length < items.length;

    const toggleAll = () => {
        if (isAllSelected) {
            setSelectedIds([]);
        } else {
            setSelectedIds(items.map(item => item[idField]));
        }
    };

    const toggleItem = (id: string) => {
        setSelectedIds(prev => 
            prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
        );
    };

    return {
        selectedIds,
        isAllSelected,
        isIndeterminate,
        toggleAll,
        toggleItem,
        setSelectedIds
    };
}
