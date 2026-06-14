'use client';
import { useState } from 'react';

interface Props {
  onSelect: (seatType: string, seatNumber: string, fee: number) => void;
  fees: Record<string, number>;
}

type SeatType = 'window' | 'middle' | 'aisle' | 'exit' | 'bulkhead' | 'occupied' | 'selected';

const ROWS  = 30;
const COLS  = ['A', 'B', 'C', 'D', 'E', 'F'] as const;
const EXIT_ROWS      = new Set([12, 20]);
const BULKHEAD_ROWS  = new Set([1, 2, 3]);

function colType(col: string, row: number): SeatType {
  if (BULKHEAD_ROWS.has(row)) return 'bulkhead';
  if (EXIT_ROWS.has(row))     return 'exit';
  if (col === 'A' || col === 'F') return 'window';
  if (col === 'B' || col === 'E') return 'middle';
  return 'aisle';
}

function seatTypeToApi(t: SeatType): string {
  if (t === 'bulkhead') return 'BULKHEAD';
  if (t === 'exit')     return 'EXIT_ROW';
  if (t === 'window')   return 'WINDOW';
  if (t === 'middle')   return 'MIDDLE';
  return 'AISLE';
}

const OCCUPIED_SEED = new Set(
  ['1A','2F','3B','5C','5D','7A','8F','9B','10E','11C','13A','14D','15F','16B','17C','18E','19A','21F','22C','23B','24D','25A','26F','27C','28B','29E','30A','30F']
);

const TYPE_CLASS: Record<SeatType, string> = {
  window:   'seat-window',
  middle:   'seat-middle',
  aisle:    'seat-aisle',
  exit:     'seat-exit',
  bulkhead: 'seat-bulkhead',
  occupied: 'seat-occupied',
  selected: 'seat-selected',
};

const FEE_MAP: Record<SeatType, keyof typeof fees_placeholder> = {
  window: 'WINDOW', middle: 'MIDDLE', aisle: 'AISLE',
  exit: 'EXIT_ROW', bulkhead: 'BULKHEAD', occupied: 'MIDDLE', selected: 'WINDOW',
};

const fees_placeholder = { WINDOW: 18, MIDDLE: 0, AISLE: 15, EXIT_ROW: 39, BULKHEAD: 29 };

export default function SeatMap({ onSelect, fees }: Props) {
  const [selected, setSelected] = useState<string | null>(null);

  function handleClick(row: number, col: string, type: SeatType) {
    if (type === 'occupied') return;
    const seat = `${row}${col}`;
    const apiType = seatTypeToApi(type);
    const fee = fees[apiType] ?? 0;
    setSelected(seat);
    onSelect(apiType, seat, fee);
  }

  const feeLookup = Object.keys(fees).length > 0 ? fees : fees_placeholder;

  return (
    <div className="select-none">
      {/* Legend */}
      <div className="flex flex-wrap gap-3 mb-5 text-xs">
        {[
          { cls: 'seat-window',   label: `Window ($${feeLookup['WINDOW'] ?? 18})` },
          { cls: 'seat-aisle',    label: `Aisle ($${feeLookup['AISLE'] ?? 15})` },
          { cls: 'seat-middle',   label: `Middle (Free)` },
          { cls: 'seat-exit',     label: `Exit Row ($${feeLookup['EXIT_ROW'] ?? 39})` },
          { cls: 'seat-bulkhead', label: `Bulkhead ($${feeLookup['BULKHEAD'] ?? 29})` },
          { cls: 'seat-occupied', label: 'Occupied' },
          { cls: 'seat-selected', label: 'Your selection' },
        ].map(({ cls, label }) => (
          <div key={label} className="flex items-center gap-1.5">
            <div className={`seat ${cls} w-5 h-5 !cursor-default pointer-events-none`} />
            <span className="text-gray-400">{label}</span>
          </div>
        ))}
      </div>

      {/* Column header */}
      <div className="overflow-x-auto pb-2">
        <div className="inline-block min-w-max">
          <div className="flex items-center gap-1 mb-2 pl-8">
            {COLS.map((col, i) => (
              <>
                {i === 3 && <div key="gap" className="w-5" />}
                <div key={col} className="w-7 text-center text-xs font-semibold text-gray-500">{col}</div>
              </>
            ))}
          </div>

          {/* Rows */}
          <div className="flex flex-col gap-1">
            {Array.from({ length: ROWS }, (_, ri) => {
              const row = ri + 1;
              const isExit = EXIT_ROWS.has(row);
              return (
                <div key={row} className="flex items-center gap-1">
                  <div className="w-7 text-right text-xs text-gray-600 pr-1 font-mono">{row}</div>
                  {COLS.map((col, ci) => {
                    const seatId = `${row}${col}`;
                    const isOccupied = OCCUPIED_SEED.has(seatId);
                    const isSelected = selected === seatId;
                    const baseType = colType(col, row);
                    const type: SeatType = isSelected ? 'selected' : isOccupied ? 'occupied' : baseType;

                    return (
                      <>
                        {ci === 3 && <div key={`gap-${row}`} className="w-5 flex items-center justify-center">
                          <div className="w-px h-full bg-white/5" />
                        </div>}
                        <div
                          key={seatId}
                          className={`seat ${TYPE_CLASS[type]}`}
                          title={`${seatId} — ${seatTypeToApi(baseType).replace('_', ' ')}`}
                          onClick={() => handleClick(row, col, baseType)}
                        />
                      </>
                    );
                  })}
                  {isExit && <div className="ml-2 text-xs text-amber-400 font-medium">EXIT</div>}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {selected && (
        <div className="mt-4 glass rounded-xl px-4 py-3 text-sm text-emerald-400 border border-emerald-500/30 animate-fade-in">
          Seat <strong>{selected}</strong> selected
          {' — '}
          {(() => {
            const row = parseInt(selected);
            const col = selected.slice(-1);
            const bt = colType(col, row);
            const apiT = seatTypeToApi(bt);
            const fee = feeLookup[apiT as keyof typeof feeLookup] ?? 0;
            return fee === 0 ? 'Free' : `+$${fee}`;
          })()}
        </div>
      )}
    </div>
  );
}
