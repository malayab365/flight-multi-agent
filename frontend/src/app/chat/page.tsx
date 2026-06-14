'use client';
import { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Plane, Zap } from 'lucide-react';
import { sendChat } from '@/lib/api';

interface Message { role: 'user' | 'assistant'; content: string; agent?: string; }

const QUICK_PROMPTS = [
  'Find flights from JFK to LHR on 2026-08-01',
  'What is the weather like in Tokyo?',
  'Track the price for NYC to London under $250',
  'What travel tips do you have for Paris?',
];

const AGENT_COLORS: Record<string, string> = {
  search_agent:    'text-sky-400',
  booking_agent:   'text-emerald-400',
  ancillary_agent: 'text-purple-400',
  price_agent:     'text-amber-400',
  info_agent:      'text-teal-400',
};

const AGENT_LABELS: Record<string, string> = {
  search_agent:    'Search Agent',
  booking_agent:   'Booking Agent',
  ancillary_agent: 'Ancillary Agent',
  price_agent:     'Price Agent',
  info_agent:      'Info Agent',
};

function TypingIndicator() {
  return (
    <div className="flex items-end gap-2 animate-fade-in">
      <div className="w-8 h-8 rounded-full bg-indigo-600/30 border border-indigo-500/30 flex items-center justify-center shrink-0">
        <Bot className="w-4 h-4 text-indigo-400" />
      </div>
      <div className="glass rounded-2xl rounded-bl-sm px-4 py-3 border border-white/8">
        <div className="flex items-center gap-1.5">
          <div className="typing-dot w-2 h-2 rounded-full bg-indigo-400" />
          <div className="typing-dot w-2 h-2 rounded-full bg-indigo-400" />
          <div className="typing-dot w-2 h-2 rounded-full bg-indigo-400" />
        </div>
      </div>
    </div>
  );
}

export default function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  async function send(text: string) {
    if (!text.trim() || loading) return;
    const userMsg: Message = { role: 'user', content: text.trim() };
    setMessages(prev => [...prev, userMsg]);
    setInput(''); setLoading(true); setError('');

    try {
      const history = messages.map(m => ({ role: m.role, content: m.content }));
      const res = await sendChat(text.trim(), history);
      setMessages(prev => [...prev, { role: 'assistant', content: res.response, agent: res.agent }]);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Something went wrong');
      setMessages(prev => prev.slice(0, -1));
    } finally {
      setLoading(false);
    }
  }

  function handleSubmit(e: React.FormEvent) { e.preventDefault(); send(input); }

  return (
    <div className="flex flex-col h-screen pt-16">
      {/* Header */}
      <div className="glass-dark border-b border-white/5 px-4 py-3 flex items-center gap-3">
        <div className="w-9 h-9 rounded-full bg-gradient-to-br from-indigo-600 to-purple-700 flex items-center justify-center">
          <Bot className="w-5 h-5 text-white" />
        </div>
        <div>
          <div className="font-bold text-white text-sm">AeroLink AI</div>
          <div className="text-xs text-gray-400 flex items-center gap-1">
            <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" /> Multi-agent flight assistant
          </div>
        </div>
        <div className="ml-auto flex items-center gap-1.5 text-xs text-gray-500">
          <Zap className="w-3.5 h-3.5 text-amber-400" /> Powered by OpenRouter
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full gap-6 animate-fade-in">
            <div className="text-center">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-600 to-purple-700 flex items-center justify-center mx-auto mb-4 glow-primary">
                <Plane className="w-8 h-8 text-white" />
              </div>
              <h2 className="text-xl font-bold text-white mb-2">How can I help you today?</h2>
              <p className="text-gray-400 text-sm max-w-sm">
                I can search flights, handle bookings, track prices, suggest seats, and tell you about destinations.
              </p>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 w-full max-w-lg">
              {QUICK_PROMPTS.map(p => (
                <button key={p} onClick={() => send(p)}
                  className="glass rounded-xl px-4 py-3 text-sm text-gray-300 hover:text-white text-left border border-white/8 hover:border-indigo-500/40 transition-all card-hover">
                  {p}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((msg, i) => (
          <div key={i} className={`flex items-end gap-2 animate-slide-up ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}>
            {/* Avatar */}
            <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${
              msg.role === 'user'
                ? 'bg-indigo-600/30 border border-indigo-500/30'
                : 'bg-purple-600/30 border border-purple-500/30'
            }`}>
              {msg.role === 'user'
                ? <User className="w-4 h-4 text-indigo-400" />
                : <Bot className="w-4 h-4 text-purple-400" />
              }
            </div>

            {/* Bubble */}
            <div className={`max-w-[75%] ${msg.role === 'user' ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
              {msg.role === 'assistant' && msg.agent && AGENT_LABELS[msg.agent] && (
                <div className={`text-xs font-medium ${AGENT_COLORS[msg.agent] ?? 'text-gray-400'} px-1`}>
                  {AGENT_LABELS[msg.agent]}
                </div>
              )}
              <div className={`px-4 py-3 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap ${
                msg.role === 'user'
                  ? 'bg-indigo-600/40 border border-indigo-500/30 text-white rounded-br-sm'
                  : 'glass border border-white/8 text-gray-200 rounded-bl-sm'
              }`}>
                {msg.content}
              </div>
            </div>
          </div>
        ))}

        {loading && <TypingIndicator />}

        {error && (
          <div className="text-center text-sm text-red-400 py-2">{error}</div>
        )}

        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div className="glass-dark border-t border-white/5 px-4 py-4">
        <form onSubmit={handleSubmit} className="max-w-3xl mx-auto flex items-end gap-3">
          <div className="flex-1 relative">
            <textarea
              value={input}
              onChange={e => { setInput(e.target.value); e.target.style.height = 'auto'; e.target.style.height = `${Math.min(e.target.scrollHeight, 120)}px`; }}
              onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(input); } }}
              placeholder="Ask about flights, bookings, prices, or destinations…"
              rows={1}
              className="input-dark w-full rounded-2xl px-4 py-3 text-sm resize-none overflow-hidden"
            />
          </div>
          <button type="submit" disabled={!input.trim() || loading}
            className="btn-primary w-11 h-11 rounded-xl flex items-center justify-center shrink-0 disabled:opacity-40 disabled:cursor-not-allowed">
            <Send className="w-4 h-4 text-white relative z-10" />
          </button>
        </form>
        <p className="text-center text-xs text-gray-600 mt-2">Press Enter to send · Shift+Enter for new line</p>
      </div>
    </div>
  );
}
