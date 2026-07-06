"use client";

import { useEffect } from "react";

export default function Toast({ message, onClose }: { message: string, onClose: () => void }) {
    useEffect(() => {
        const timer = setTimeout(onClose, 3000);
        return () => clearTimeout(timer);
    }, [onClose]);

    return (
        <div className="fixed bottom-6 right-6 z-50 toast-fade-in-up-out">
            <div className="bg-ink text-canvas px-6 py-4 rounded-xl shadow-lg font-bold">
                {message}
            </div>
        </div>
    );
}