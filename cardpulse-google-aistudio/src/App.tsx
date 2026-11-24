import React, { useState, useEffect, useMemo } from 'react';
import {
  Bell,
  Settings,
  Plus,
  CheckCircle2,
  CreditCard,
  Calendar,
  Repeat,
  Wallet,
  Trash2,
  Edit3,
  X,
  Upload,
  Sparkles,
  AlertTriangle,
  Clock,
  ShieldCheck,
  Undo2,
} from 'lucide-react';

export type RecurrenceUnit = 'days' | 'weeks' | 'months';

export type BankPresetId =
  | 'hsbc'
  | 'citi'
  | 'mox'
  | 'sc'
  | 'hangseng'
  | 'zabank'
  | 'custom';

export interface CreditCardItem {
  id: string;
  bankPreset: BankPresetId;
  bankName: string;
  cardLabel: string;
  last4: string;
  dueDate: string; // YYYY-MM-DD
  paymentMethod: string;
  recurrenceEvery: number;
  recurrenceUnit: RecurrenceUnit;
  customImage?: string; // base64 compressed data URL
}

export interface ReminderSettings {
  fiveDaysBefore: boolean;
  twoDaysBefore: boolean;
  onDueDate: boolean;
  browserNotifications: boolean;
}

interface BankTheme {
  id: BankPresetId;
  name: string;
  shortTag: string;
  network: 'VISA' | 'Mastercard' | 'UnionPay';
  gradient: string;
  accent: string;
  textColor: string;
  badgeBg: string;
}

const BANK_THEMES: Record<BankPresetId, BankTheme> = {
  hsbc: {
    id: 'hsbc',
    name: 'HSBC',
    shortTag: 'HSBC PREMIER / RED',
    network: 'Mastercard',
    gradient: 'from-[#1f0609] via-[#7f1d1d] to-[#dc2626]',
    accent: '#ef4444',
    textColor: 'text-white',
    badgeBg: 'bg-red-500/20 border-red-400/40 text-red-200',
  },
  citi: {
    id: 'citi',
    name: 'Citi',
    shortTag: 'CITI CASH BACK / REWARDS',
    network: 'VISA',
    gradient: 'from-[#04152d] via-[#0c3b75] to-[#0284c7]',
    accent: '#38bdf8',
    textColor: 'text-white',
    badgeBg: 'bg-sky-500/20 border-sky-400/40 text-sky-200',
  },
  mox: {
    id: 'mox',
    name: 'Mox Bank',
    shortTag: 'MOX CARD • DIGITAL',
    network: 'Mastercard',
    gradient: 'from-[#042f2e] via-[#0d9488] to-[#2dd4bf]',
    accent: '#2dd4bf',
    textColor: 'text-white',
    badgeBg: 'bg-teal-500/20 border-teal-300/40 text-teal-100',
  },
  sc: {
    id: 'sc',
    name: 'Standard Chartered',
    shortTag: 'SC SIMPLY CASH / CATHAY',
    network: 'Mastercard',
    gradient: 'from-[#062038] via-[#0f4c81] to-[#15803d]',
    accent: '#4ade80',
    textColor: 'text-white',
    badgeBg: 'bg-emerald-500/20 border-emerald-400/40 text-emerald-200',
  },
  hangseng: {
    id: 'hangseng',
    name: 'Hang Seng Bank',
    shortTag: 'HANG SENG MMPOWER',
    network: 'Mastercard',
    gradient: 'from-[#032219] via-[#065f46] to-[#10b981]',
    accent: '#34d399',
    textColor: 'text-white',
    badgeBg: 'bg-emerald-500/20 border-emerald-300/40 text-emerald-100',
  },
  zabank: {
    id: 'zabank',
    name: 'ZA Bank',
    shortTag: 'ZA CARD • CUSTOM',
    network: 'VISA',
    gradient: 'from-[#052e2b] via-[#0f766e] to-[#84cc16]',
    accent: '#a3e635',
    textColor: 'text-white',
    badgeBg: 'bg-lime-500/20 border-lime-300/40 text-lime-100',
  },
  custom: {
    id: 'custom',
    name: 'Custom Bank',
    shortTag: 'OBSIDIAN EDITION',
    network: 'VISA',
    gradient: 'from-[#090d16] via-[#1e293b] to-[#0e7490]',
    accent: '#22d3ee',
    textColor: 'text-white',
    badgeBg: 'bg-cyan-500/20 border-cyan-400/40 text-cyan-200',
  },
};

const PAYMENT_METHODS = [
  'FPS (Faster Payment)',
  'Autopay (Direct Debit)',
  'Bank App Transfer',
  'PPS',
  'AlipayHK / WeChat Pay',
  'Manual Check',
];

const STORAGE_CARDS_KEY = 'cardpulse_cards_v2';
const STORAGE_SETTINGS_KEY = 'cardpulse_settings_v2';

function formatLocalYYYYMMDD(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function addDaysToToday(days: number): string {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + days);
  return formatLocalYYYYMMDD(d);
}

function getDaysUntil(dueDateStr: string): number {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const [y, m, d] = dueDateStr.split('-').map(Number);
  const due = new Date(y, m - 1, d);
  due.setHours(0, 0, 0, 0);
  return Math.round((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
}

function advanceDateByRecurrence(
  dueDateStr: string,
  every: number,
  unit: RecurrenceUnit
): string {
  const [y, m, d] = dueDateStr.split('-').map(Number);
  const base = new Date(y, m - 1, d);
  if (unit === 'days') {
    base.setDate(base.getDate() + every);
    return formatLocalYYYYMMDD(base);
  }
  if (unit === 'weeks') {
    base.setDate(base.getDate() + every * 7);
    return formatLocalYYYYMMDD(base);
  }
  // Months with month-end clamping (e.g., Jan 31 + 1 month -> Feb 28/29)
  const targetMonthIndex = base.getMonth() + every;
  const targetYear = base.getFullYear() + Math.floor(targetMonthIndex / 12);
  const normalizedMonth = ((targetMonthIndex % 12) + 12) % 12;
  const lastDayOfTargetMonth = new Date(targetYear, normalizedMonth + 1, 0).getDate();
  const clampedDay = Math.min(d, lastDayOfTargetMonth);
  return formatLocalYYYYMMDD(new Date(targetYear, normalizedMonth, clampedDay));
}

function formatDisplayDate(dueDateStr: string): string {
  const [y, m, d] = dueDateStr.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

const INITIAL_CARDS: CreditCardItem[] = [
  {
    id: 'seed-hsbc',
    bankPreset: 'hsbc',
    bankName: 'HSBC',
    cardLabel: 'Red Mastercard',
    last4: '4821',
    dueDate: addDaysToToday(0),
    paymentMethod: 'FPS (Faster Payment)',
    recurrenceEvery: 1,
    recurrenceUnit: 'months',
  },
  {
    id: 'seed-mox',
    bankPreset: 'mox',
    bankName: 'Mox Bank',
    cardLabel: 'Mox Credit',
    last4: '9034',
    dueDate: addDaysToToday(2),
    paymentMethod: 'Autopay (Direct Debit)',
    recurrenceEvery: 1,
    recurrenceUnit: 'months',
  },
  {
    id: 'seed-citi',
    bankPreset: 'citi',
    bankName: 'Citi',
    cardLabel: 'Cash Back Visa',
    last4: '7719',
    dueDate: addDaysToToday(5),
    paymentMethod: 'Bank App Transfer',
    recurrenceEvery: 1,
    recurrenceUnit: 'months',
  },
  {
    id: 'seed-sc',
    bankPreset: 'sc',
    bankName: 'Standard Chartered',
    cardLabel: 'Simply Cash',
    last4: '3310',
    dueDate: addDaysToToday(14),
    paymentMethod: 'FPS (Faster Payment)',
    recurrenceEvery: 1,
    recurrenceUnit: 'months',
  },
];

const DEFAULT_SETTINGS: ReminderSettings = {
  fiveDaysBefore: true,
  twoDaysBefore: true,
  onDueDate: true,
  browserNotifications: false,
};

export default function App() {
  const [cards, setCards] = useState<CreditCardItem[]>(() => {
    try {
      const raw = localStorage.getItem(STORAGE_CARDS_KEY);
      if (raw) return JSON.parse(raw);
    } catch {}
    return INITIAL_CARDS;
  });

  const [settings, setSettings] = useState<ReminderSettings>(() => {
    try {
      const raw = localStorage.getItem(STORAGE_SETTINGS_KEY);
      if (raw) return JSON.parse(raw);
    } catch {}
    return DEFAULT_SETTINGS;
  });

  const [filterMode, setFilterMode] = useState<'all' | 'urgent'>('all');
  const [isCardModalOpen, setIsCardModalOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [editingCard, setEditingCard] = useState<CreditCardItem | null>(null);

  // Toast / Heads-up notification banner
  const [toast, setToast] = useState<{
    title: string;
    message: string;
    undoCard?: CreditCardItem;
  } | null>(null);

  // Form states
  const [bankPreset, setBankPreset] = useState<BankPresetId>('hsbc');
  const [bankName, setBankName] = useState('HSBC');
  const [cardLabel, setCardLabel] = useState('');
  const [last4, setLast4] = useState('');
  const [dueDate, setDueDate] = useState(addDaysToToday(7));
  const [paymentMethod, setPaymentMethod] = useState(PAYMENT_METHODS[0]);
  const [customMethod, setCustomMethod] = useState('');
  const [recurrenceEvery, setRecurrenceEvery] = useState(1);
  const [recurrenceUnit, setRecurrenceUnit] = useState<RecurrenceUnit>('months');
  const [customImage, setCustomImage] = useState<string | undefined>(undefined);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_CARDS_KEY, JSON.stringify(cards));
    } catch {}
  }, [cards]);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_SETTINGS_KEY, JSON.stringify(settings));
    } catch {}
  }, [settings]);

  const sortedCards = useMemo(() => {
    return [...cards]
      .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
      .filter((c) => {
        if (filterMode === 'urgent') {
          return getDaysUntil(c.dueDate) <= 7;
        }
        return true;
      });
  }, [cards, filterMode]);

  const activeReminders = useMemo(() => {
    const list: { card: CreditCardItem; daysLeft: number; label: string; severity: 'due' | 'warn' | 'info' }[] = [];
    for (const card of cards) {
      const d = getDaysUntil(card.dueDate);
      if (d < 0) {
        list.push({
          card,
          daysLeft: d,
          label: `Overdue by ${Math.abs(d)}d — Immediate payment required`,
          severity: 'due',
        });
      } else if (d === 0 && settings.onDueDate) {
        list.push({
          card,
          daysLeft: 0,
          label: 'Final payment reminder (Due today)',
          severity: 'due',
        });
      } else if (d > 0 && d <= 2 && settings.twoDaysBefore) {
        list.push({
          card,
          daysLeft: d,
          label: `Payment still outstanding (${d}d left)`,
          severity: 'warn',
        });
      } else if (d > 2 && d <= 5 && settings.fiveDaysBefore) {
        list.push({
          card,
          daysLeft: d,
          label: `Payment coming up (${d}d left)`,
          severity: 'info',
        });
      }
    }
    return list.sort((a, b) => a.daysLeft - b.daysLeft);
  }, [cards, settings]);

  const nextDueSummary = useMemo(() => {
    if (cards.length === 0) return '—';
    const nearest = [...cards].sort((a, b) => a.dueDate.localeCompare(b.dueDate))[0];
    const diff = getDaysUntil(nearest.dueDate);
    if (diff < 0) return `${Math.abs(diff)}d late`;
    if (diff === 0) return 'Today';
    return `In ${diff}d`;
  }, [cards]);

  const urgentCount = useMemo(
    () => cards.filter((c) => getDaysUntil(c.dueDate) <= 7).length,
    [cards]
  );

  function openAddModal() {
    setEditingCard(null);
    setBankPreset('hsbc');
    setBankName('HSBC');
    setCardLabel('');
    setLast4('');
    setDueDate(addDaysToToday(7));
    setPaymentMethod(PAYMENT_METHODS[0]);
    setCustomMethod('');
    setRecurrenceEvery(1);
    setRecurrenceUnit('months');
    setCustomImage(undefined);
    setIsCardModalOpen(true);
  }

  function openEditModal(card: CreditCardItem) {
    setEditingCard(card);
    setBankPreset(card.bankPreset);
    setBankName(card.bankName);
    setCardLabel(card.cardLabel);
    setLast4(card.last4);
    setDueDate(card.dueDate);
    if (PAYMENT_METHODS.includes(card.paymentMethod)) {
      setPaymentMethod(card.paymentMethod);
      setCustomMethod('');
    } else {
      setPaymentMethod('OTHER');
      setCustomMethod(card.paymentMethod);
    }
    setRecurrenceEvery(card.recurrenceEvery);
    setRecurrenceUnit(card.recurrenceUnit);
    setCustomImage(card.customImage);
    setIsCardModalOpen(true);
  }

  function handlePresetSelect(preset: BankPresetId) {
    setBankPreset(preset);
    if (preset !== 'custom') {
      setBankName(BANK_THEMES[preset].name);
    } else if (Object.values(BANK_THEMES).some((t) => t.name === bankName)) {
      setBankName('');
    }
  }

  function handleImageUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const targetW = 640;
        const targetH = Math.round(targetW / 1.586);
        canvas.width = targetW;
        canvas.height = targetH;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        // Cover crop to 1.586:1 ISO/IEC 7810 ID-1 credit card aspect ratio
        const srcRatio = img.width / img.height;
        const dstRatio = targetW / targetH;
        let sx = 0,
          sy = 0,
          sw = img.width,
          sh = img.height;
        if (srcRatio > dstRatio) {
          sw = img.height * dstRatio;
          sx = (img.width - sw) / 2;
        } else {
          sh = img.width / dstRatio;
          sy = (img.height - sh) / 2;
        }
        ctx.drawImage(img, sx, sy, sw, sh, 0, 0, targetW, targetH);
        const compressed = canvas.toDataURL('image/jpeg', 0.82);
        setCustomImage(compressed);
      };
      img.src = ev.target?.result as string;
    };
    reader.readAsDataURL(file);
  }

  function handleSaveCard(e: React.FormEvent) {
    e.preventDefault();
    const resolvedBankName = bankName.trim() || BANK_THEMES[bankPreset].name;
    const resolvedMethod =
      paymentMethod === 'OTHER'
        ? customMethod.trim() || 'Manual Payment'
        : paymentMethod;

    const payload: CreditCardItem = {
      id: editingCard ? editingCard.id : `card-${Date.now()}`,
      bankPreset,
      bankName: resolvedBankName,
      cardLabel: cardLabel.trim() || 'Credit Card',
      last4: last4.replace(/\D/g, '').slice(0, 4) || '8888',
      dueDate,
      paymentMethod: resolvedMethod,
      recurrenceEvery: Math.max(1, Number(recurrenceEvery) || 1),
      recurrenceUnit,
      customImage,
    };

    if (editingCard) {
      setCards((prev) => prev.map((c) => (c.id === editingCard.id ? payload : c)));
      showToast('Card Updated', `${payload.bankName} settings saved.`);
    } else {
      setCards((prev) => [...prev, payload]);
      showToast('Card Added', `${payload.bankName} added to your wallet.`);
    }
    setIsCardModalOpen(false);
  }

  function handleMarkPaid(card: CreditCardItem) {
    const nextDate = advanceDateByRecurrence(
      card.dueDate,
      card.recurrenceEvery,
      card.recurrenceUnit
    );
    const snapshot = { ...card };
    setCards((prev) =>
      prev.map((c) => (c.id === card.id ? { ...c, dueDate: nextDate } : c))
    );
    showToast(
      'Payment Logged',
      `${card.bankName} advanced to ${formatDisplayDate(nextDate)}`,
      snapshot
    );
  }

  function handleUndo(snapshot: CreditCardItem) {
    setCards((prev) => prev.map((c) => (c.id === snapshot.id ? snapshot : c)));
    setToast(null);
  }

  function handleDeleteCard(id: string) {
    setCards((prev) => prev.filter((c) => c.id !== id));
    setIsCardModalOpen(false);
    showToast('Card Removed', 'Credit card deleted from tracker.');
  }

  function showToast(title: string, message: string, undoCard?: CreditCardItem) {
    setToast({ title, message, undoCard });
  }

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 5000);
    return () => clearTimeout(timer);
  }, [toast]);

  async function toggleBrowserNotifications() {
    if (!settings.browserNotifications) {
      if ('Notification' in window) {
        const perm = await Notification.requestPermission();
        if (perm === 'granted') {
          setSettings((s) => ({ ...s, browserNotifications: true }));
          showToast('Alerts Enabled', 'Native push notifications activated.');
        } else {
          showToast('Permission Blocked', 'Allow notifications in browser settings.');
        }
      }
    } else {
      setSettings((s) => ({ ...s, browserNotifications: false }));
    }
  }

  function triggerTestReminder() {
    const sample = activeReminders[0];
    const title = sample
      ? `CardPulse • ${sample.card.bankName}`
      : 'CardPulse • Payment coming up';
    const body = sample
      ? sample.label
      : 'HSBC Red Mastercard payment is due in 5 days via FPS.';

    showToast(title, body);
    if (
      settings.browserNotifications &&
      'Notification' in window &&
      Notification.permission === 'granted'
    ) {
      new Notification(title, { body });
    }
  }

  return (
    <div className="min-h-screen bg-[#04070d] flex justify-center">
      {/* Android Mobile Viewport Shell */}
      <div className="w-full max-w-[440px] min-h-screen bg-[#080c14] border-x border-slate-800/70 relative flex flex-col pb-24 shadow-2xl shadow-cyan-950/20">
        {/* Android Status Bar Simulation */}
        <div className="px-5 pt-2.5 pb-1 flex items-center justify-between text-[11px] font-mono text-slate-400">
          <span>{new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
          <div className="flex items-center gap-1.5">
            <span className="inline-block w-1.5 h-1.5 rounded-full bg-cyan-400 animate-pulse" />
            <span className="text-cyan-400/90">5G</span>
            <span>100%</span>
          </div>
        </div>

        {/* App Top Header */}
        <header className="px-5 pt-2 pb-4 flex items-center justify-between border-b border-slate-800/70 bg-[#080c14]/90 backdrop-blur-md sticky top-0 z-20">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-cyan-400 to-blue-600 p-[1px] shadow-lg shadow-cyan-500/20">
              <div className="w-full h-full rounded-[11px] bg-[#080c14] flex items-center justify-center">
                <CreditCard className="w-5 h-5 text-cyan-400" />
              </div>
            </div>
            <div>
              <h1 className="text-base font-bold tracking-tight text-white flex items-center gap-1.5">
                CardPulse
                <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-cyan-500/10 text-cyan-400 border border-cyan-500/30">
                  PAY
                </span>
              </h1>
              <p className="text-[11px] text-slate-400">Smart Billing Telemetry</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setIsSettingsOpen(true)}
              className="relative p-2.5 rounded-xl bg-slate-900/90 border border-slate-800 text-slate-300 hover:text-cyan-400 hover:border-cyan-500/40 transition"
              aria-label="Reminders"
            >
              <Bell className="w-4 h-4" />
              {activeReminders.length > 0 && (
                <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-rose-500 text-[10px] font-bold text-white flex items-center justify-center shadow">
                  {activeReminders.length}
                </span>
              )}
            </button>
            <button
              onClick={() => setIsSettingsOpen(true)}
              className="p-2.5 rounded-xl bg-slate-900/90 border border-slate-800 text-slate-300 hover:text-cyan-400 hover:border-cyan-500/40 transition"
              aria-label="Settings"
            >
              <Settings className="w-4 h-4" />
            </button>
          </div>
        </header>

        {/* Heads-Up Toast Notification */}
        {toast && (
          <div className="mx-4 mt-3 p-3.5 rounded-2xl bg-slate-900/95 border border-cyan-500/40 shadow-xl shadow-cyan-950/50 flex items-center justify-between gap-3 animate-fade-in z-30">
            <div className="flex items-start gap-2.5">
              <div className="mt-0.5 p-1.5 rounded-lg bg-cyan-500/15 text-cyan-400">
                <Bell className="w-4 h-4" />
              </div>
              <div>
                <div className="text-xs font-semibold text-white">{toast.title}</div>
                <div className="text-[11px] text-slate-300 mt-0.5">{toast.message}</div>
              </div>
            </div>
            {toast.undoCard ? (
              <button
                onClick={() => handleUndo(toast.undoCard!)}
                className="px-2.5 py-1.5 rounded-lg bg-cyan-500/20 text-cyan-300 text-xs font-medium flex items-center gap-1 hover:bg-cyan-500/30 shrink-0"
              >
                <Undo2 className="w-3.5 h-3.5" />
                Undo
              </button>
            ) : (
              <button
                onClick={() => setToast(null)}
                className="text-slate-400 hover:text-white p-1"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        )}

        {/* Telemetry Overview Strip */}
        <section className="px-5 pt-4">
          <div className="grid grid-cols-3 gap-2.5">
            <div className="p-3 rounded-2xl bg-[#0f172a]/90 border border-slate-800/80">
              <div className="text-[10px] uppercase tracking-wider text-slate-400">Active Cards</div>
              <div className="text-lg font-bold font-mono text-white mt-0.5">{cards.length}</div>
            </div>
            <div className="p-3 rounded-2xl bg-[#0f172a]/90 border border-slate-800/80">
              <div className="text-[10px] uppercase tracking-wider text-slate-400">Next Due</div>
              <div className="text-lg font-bold font-mono text-cyan-400 mt-0.5">{nextDueSummary}</div>
            </div>
            <div className="p-3 rounded-2xl bg-[#0f172a]/90 border border-slate-800/80">
              <div className="text-[10px] uppercase tracking-wider text-slate-400">Due ≤ 7d</div>
              <div className="text-lg font-bold font-mono text-amber-400 mt-0.5">{urgentCount}</div>
            </div>
          </div>
        </section>

        {/* Active Reminders Alert Banner */}
        {activeReminders.length > 0 && (
          <section className="px-5 pt-3.5">
            <div className="p-3.5 rounded-2xl bg-gradient-to-r from-amber-500/10 via-rose-500/10 to-slate-900 border border-amber-500/30">
              <div className="flex items-center justify-between mb-2">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-amber-300 flex items-center gap-1.5">
                  <AlertTriangle className="w-3.5 h-3.5" />
                  Active Reminders ({activeReminders.length})
                </span>
                <button
                  onClick={() => setIsSettingsOpen(true)}
                  className="text-[11px] text-cyan-400 hover:underline"
                >
                  Configure
                </button>
              </div>
              <div className="space-y-1.5">
                {activeReminders.map((item) => (
                  <div
                    key={item.card.id}
                    className="flex items-center justify-between text-xs bg-slate-950/60 px-2.5 py-1.5 rounded-lg border border-slate-800/80"
                  >
                    <span className="font-medium text-slate-200 truncate pr-2">
                      {item.card.bankName}
                    </span>
                    <span
                      className={`font-mono text-[11px] shrink-0 ${
                        item.severity === 'due'
                          ? 'text-rose-400'
                          : item.severity === 'warn'
                          ? 'text-amber-400'
                          : 'text-cyan-300'
                      }`}
                    >
                      {item.label}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </section>
        )}

        {/* Section Filter Header */}
        <div className="px-5 pt-5 pb-3 flex items-center justify-between">
          <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
            Wallet Stack • Sorted by Due Date
          </h2>
          <div className="flex bg-slate-900 p-0.5 rounded-xl border border-slate-800 text-[11px]">
            <button
              onClick={() => setFilterMode('all')}
              className={`px-2.5 py-1 rounded-lg font-medium transition ${
                filterMode === 'all'
                  ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              All ({cards.length})
            </button>
            <button
              onClick={() => setFilterMode('urgent')}
              className={`px-2.5 py-1 rounded-lg font-medium transition ${
                filterMode === 'urgent'
                  ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              ≤ 7d ({urgentCount})
            </button>
          </div>
        </div>

        {/* Credit Cards List */}
        <main className="px-5 space-y-4 flex-1">
          {sortedCards.length === 0 ? (
            <div className="text-center py-14 px-6 rounded-3xl bg-slate-900/50 border border-dashed border-slate-800">
              <CreditCard className="w-10 h-10 text-slate-600 mx-auto mb-3" />
              <p className="text-sm font-medium text-slate-300">No credit cards in this view</p>
              <p className="text-xs text-slate-500 mt-1">
                Tap the + button below to add your bank card and payment cycle.
              </p>
            </div>
          ) : (
            sortedCards.map((card) => {
              const daysLeft = getDaysUntil(card.dueDate);
              const theme = BANK_THEMES[card.bankPreset] || BANK_THEMES.custom;

              let statusBadge = {
                text: `In ${daysLeft} days`,
                cls: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
              };
              if (daysLeft < 0) {
                statusBadge = {
                  text: `Overdue ${Math.abs(daysLeft)}d`,
                  cls: 'bg-rose-500/20 text-rose-300 border-rose-500/40 animate-pulse',
                };
              } else if (daysLeft === 0) {
                statusBadge = {
                  text: 'DUE TODAY',
                  cls: 'bg-rose-500/20 text-rose-300 border-rose-500/40',
                };
              } else if (daysLeft <= 2) {
                statusBadge = {
                  text: `${daysLeft}d left • Urgent`,
                  cls: 'bg-amber-500/20 text-amber-300 border-amber-500/40',
                };
              } else if (daysLeft <= 5) {
                statusBadge = {
                  text: `${daysLeft}d left • Soon`,
                  cls: 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40',
                };
              }

              return (
                <div
                  key={card.id}
                  className="rounded-3xl bg-[#0f172a]/95 border border-slate-800/90 overflow-hidden shadow-lg shadow-black/40 transition hover:border-slate-700"
                >
                  {/* Google Pay Style Physical Card Visual (1.586:1 Aspect Ratio) */}
                  <div
                    onClick={() => openEditModal(card)}
                    className={`relative aspect-[1.586/1] w-full p-5 flex flex-col justify-between cursor-pointer select-none overflow-hidden card-holo ${
                      !card.customImage ? `bg-gradient-to-br ${theme.gradient}` : 'bg-slate-900'
                    }`}
                  >
                    {card.customImage && (
                      <>
                        <img
                          src={card.customImage}
                          alt={card.bankName}
                          className="absolute inset-0 w-full h-full object-cover"
                        />
                        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/30 to-black/45" />
                      </>
                    )}

                    {/* Top Row: Bank Name + Urgency Pill */}
                    <div className="relative z-10 flex items-start justify-between gap-2">
                      <div>
                        <div className="text-base font-bold tracking-tight text-white drop-shadow">
                          {card.bankName}
                        </div>
                        <div className="text-[10px] font-mono uppercase tracking-widest text-white/75">
                          {card.cardLabel || theme.shortTag}
                        </div>
                      </div>

                      <span
                        className={`px-2.5 py-1 rounded-full text-[11px] font-mono font-semibold border backdrop-blur-md ${statusBadge.cls}`}
                      >
                        {statusBadge.text}
                      </span>
                    </div>

                    {/* Middle Row: EMV Metallic Chip + Contactless Waves */}
                    <div className="relative z-10 flex items-center gap-3 my-auto">
                      <div className="w-10 h-7 rounded-md emv-chip relative overflow-hidden">
                        <div className="absolute inset-x-0 top-1/2 h-[1px] bg-black/25" />
                        <div className="absolute inset-y-0 left-1/3 w-[1px] bg-black/25" />
                        <div className="absolute inset-y-0 right-1/3 w-[1px] bg-black/25" />
                      </div>
                      <svg
                        className="w-5 h-5 text-white/70"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                      >
                        <path d="M8.5 14.5A5 5 0 0 1 8.5 9.5" />
                        <path d="M11.5 16.5A8 8 0 0 1 11.5 7.5" />
                        <path d="M14.5 18.5A11 11 0 0 1 14.5 5.5" />
                      </svg>
                    </div>

                    {/* Bottom Row: Masked Card Digits + Due Date + Network */}
                    <div className="relative z-10 flex items-end justify-between">
                      <div>
                        <div className="font-mono text-sm tracking-[0.2em] text-white/90 drop-shadow">
                          •••• •••• •••• {card.last4}
                        </div>
                        <div className="text-[11px] text-white/80 mt-0.5 flex items-center gap-1.5">
                          <Calendar className="w-3 h-3 text-cyan-300" />
                          <span>Due {formatDisplayDate(card.dueDate)}</span>
                        </div>
                      </div>

                      <div className="text-right">
                        <span className="text-xs font-extrabold italic tracking-wider text-white/90">
                          {theme.network}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Card Metadata & Action Bar */}
                  <div className="p-3.5 bg-[#0b111e] flex items-center justify-between gap-2 border-t border-slate-800/80">
                    <div className="space-y-1 min-w-0">
                      <div className="flex items-center gap-1.5 text-xs text-slate-300 truncate">
                        <Wallet className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                        <span className="truncate">{card.paymentMethod}</span>
                      </div>
                      <div className="flex items-center gap-1.5 text-[11px] font-mono text-slate-400">
                        <Repeat className="w-3 h-3 text-slate-500 shrink-0" />
                        <span>
                          Every {card.recurrenceEvery}{' '}
                          {card.recurrenceEvery === 1
                            ? card.recurrenceUnit.slice(0, -1)
                            : card.recurrenceUnit}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={() => openEditModal(card)}
                        className="p-2 rounded-xl bg-slate-800/80 text-slate-300 hover:text-white hover:bg-slate-700 transition"
                        title="Edit Card"
                      >
                        <Edit3 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleMarkPaid(card)}
                        className="px-3.5 py-2 rounded-xl bg-cyan-500/15 hover:bg-cyan-500/25 text-cyan-300 border border-cyan-500/35 text-xs font-semibold flex items-center gap-1.5 transition active:scale-95"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                        Mark Paid
                      </button>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </main>

        {/* Floating Action Button (+) */}
        <button
          onClick={openAddModal}
          className="fixed bottom-6 right-6 sm:right-[calc(50%-196px)] w-14 h-14 rounded-2xl bg-gradient-to-tr from-cyan-500 to-blue-600 text-slate-950 shadow-xl shadow-cyan-500/30 flex items-center justify-center hover:scale-105 active:scale-95 transition z-30"
          aria-label="Add Credit Card"
        >
          <Plus className="w-7 h-7 stroke-[2.5]" />
        </button>

        {/* ADD / EDIT CARD BOTTOM SHEET MODAL */}
        {isCardModalOpen && (
          <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4">
            <div className="w-full max-w-[440px] max-h-[92vh] overflow-y-auto no-scrollbar rounded-t-3xl sm:rounded-3xl bg-[#0d1424] border border-slate-800 p-5 shadow-2xl">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <h3 className="text-base font-bold text-white">
                  {editingCard ? 'Edit Credit Card' : 'Add New Credit Card'}
                </h3>
                <button
                  onClick={() => setIsCardModalOpen(false)}
                  className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {/* Live Google Pay Card Preview */}
              <div className="mt-4">
                <div className="text-[11px] font-mono uppercase tracking-wider text-slate-400 mb-2">
                  Live Google Pay Card Preview
                </div>
                <div
                  className={`relative aspect-[1.586/1] w-full rounded-2xl p-4 flex flex-col justify-between overflow-hidden card-holo border border-white/15 ${
                    !customImage
                      ? `bg-gradient-to-br ${BANK_THEMES[bankPreset].gradient}`
                      : 'bg-slate-900'
                  }`}
                >
                  {customImage && (
                    <>
                      <img
                        src={customImage}
                        alt="Custom Card"
                        className="absolute inset-0 w-full h-full object-cover"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/25 to-black/40" />
                    </>
                  )}
                  <div className="relative z-10 flex justify-between items-start">
                    <div>
                      <div className="text-sm font-bold text-white">
                        {bankName || BANK_THEMES[bankPreset].name}
                      </div>
                      <div className="text-[10px] font-mono text-white/75">
                        {cardLabel || 'Credit Card'}
                      </div>
                    </div>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-black/40 text-cyan-300 border border-white/15">
                      {dueDate}
                    </span>
                  </div>
                  <div className="relative z-10 w-8 h-5 rounded emv-chip my-auto" />
                  <div className="relative z-10 flex justify-between items-end">
                    <span className="font-mono text-xs tracking-widest text-white/90">
                      •••• •••• •••• {last4 || '8888'}
                    </span>
                    <span className="text-[11px] font-bold italic text-white/90">
                      {BANK_THEMES[bankPreset].network}
                    </span>
                  </div>
                </div>
              </div>

              <form onSubmit={handleSaveCard} className="mt-4 space-y-4">
                {/* Bank Style Selector */}
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1.5">
                    Select Bank Card Style
                  </label>
                  <div className="grid grid-cols-3 gap-1.5">
                    {(Object.keys(BANK_THEMES) as BankPresetId[]).map((presetKey) => {
                      const item = BANK_THEMES[presetKey];
                      const active = bankPreset === presetKey;
                      return (
                        <button
                          type="button"
                          key={presetKey}
                          onClick={() => handlePresetSelect(presetKey)}
                          className={`px-2.5 py-2 rounded-xl text-xs font-medium border text-left truncate transition ${
                            active
                              ? 'bg-cyan-500/20 border-cyan-400 text-cyan-200'
                              : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                          }`}
                        >
                          {item.name}
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Custom Card Photo Upload */}
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1.5">
                    Custom Card Image (Optional)
                  </label>
                  <div className="flex items-center gap-2">
                    <label className="flex-1 cursor-pointer flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl bg-slate-900 border border-dashed border-slate-700 hover:border-cyan-400 text-xs text-slate-300 transition">
                      <Upload className="w-4 h-4 text-cyan-400" />
                      <span>{customImage ? 'Replace Card Photo' : 'Upload Card Photo'}</span>
                      <input
                        type="file"
                        accept="image/*"
                        onChange={handleImageUpload}
                        className="hidden"
                      />
                    </label>
                    {customImage && (
                      <button
                        type="button"
                        onClick={() => setCustomImage(undefined)}
                        className="px-3 py-2.5 rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-300 text-xs"
                      >
                        Reset
                      </button>
                    )}
                  </div>
                </div>

                {/* Bank Name & Card Nickname */}
                <div className="grid grid-cols-2 gap-2.5">
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">
                      Bank Name
                    </label>
                    <input
                      type="text"
                      required
                      value={bankName}
                      onChange={(e) => setBankName(e.target.value)}
                      placeholder="e.g., HSBC"
                      className="w-full px-3 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm text-white focus:outline-none focus:border-cyan-400"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">
                      Card Tier / Label
                    </label>
                    <input
                      type="text"
                      value={cardLabel}
                      onChange={(e) => setCardLabel(e.target.value)}
                      placeholder="e.g., Visa Signature"
                      className="w-full px-3 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm text-white focus:outline-none focus:border-cyan-400"
                    />
                  </div>
                </div>

                {/* Due Date & Last 4 Digits */}
                <div className="grid grid-cols-2 gap-2.5">
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">
                      Next Due Date
                    </label>
                    <input
                      type="date"
                      required
                      value={dueDate}
                      onChange={(e) => setDueDate(e.target.value)}
                      className="w-full px-3 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm font-mono text-white focus:outline-none focus:border-cyan-400"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">
                      Last 4 Digits
                    </label>
                    <input
                      type="text"
                      maxLength={4}
                      value={last4}
                      onChange={(e) => setLast4(e.target.value.replace(/\D/g, ''))}
                      placeholder="8888"
                      className="w-full px-3 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm font-mono text-white focus:outline-none focus:border-cyan-400"
                    />
                  </div>
                </div>

                {/* Payment Method */}
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">
                    Payment Method
                  </label>
                  <select
                    value={paymentMethod}
                    onChange={(e) => setPaymentMethod(e.target.value)}
                    className="w-full px-3 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm text-white focus:outline-none focus:border-cyan-400"
                  >
                    {PAYMENT_METHODS.map((m) => (
                      <option key={m} value={m}>
                        {m}
                      </option>
                    ))}
                    <option value="OTHER">Custom Payment Method...</option>
                  </select>
                  {paymentMethod === 'OTHER' && (
                    <input
                      type="text"
                      value={customMethod}
                      onChange={(e) => setCustomMethod(e.target.value)}
                      placeholder="Enter custom payment method"
                      className="mt-2 w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-sm text-white focus:outline-none focus:border-cyan-400"
                    />
                  )}
                </div>

                {/* Custom Recurrence */}
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">
                    Repeat Every (Custom Recurrence)
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="number"
                      min={1}
                      max={365}
                      value={recurrenceEvery}
                      onChange={(e) => setRecurrenceEvery(Number(e.target.value))}
                      className="w-24 px-3 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-sm font-mono text-white focus:outline-none focus:border-cyan-400"
                    />
                    <div className="flex-1 grid grid-cols-3 gap-1.5 bg-slate-900 p-1 rounded-xl border border-slate-800">
                      {(['days', 'weeks', 'months'] as RecurrenceUnit[]).map((u) => (
                        <button
                          type="button"
                          key={u}
                          onClick={() => setRecurrenceUnit(u)}
                          className={`py-1.5 rounded-lg text-xs font-medium capitalize transition ${
                            recurrenceUnit === u
                              ? 'bg-cyan-500 text-slate-950 font-bold'
                              : 'text-slate-400 hover:text-white'
                          }`}
                        >
                          {u}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                {/* Submit & Delete Actions */}
                <div className="pt-2 flex items-center gap-2.5">
                  {editingCard && (
                    <button
                      type="button"
                      onClick={() => handleDeleteCard(editingCard.id)}
                      className="p-3 rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-400 hover:bg-rose-500/25"
                      title="Delete Card"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                  <button
                    type="submit"
                    className="flex-1 py-3 rounded-xl bg-gradient-to-r from-cyan-400 to-blue-500 text-slate-950 font-bold text-sm shadow-lg shadow-cyan-500/25 hover:opacity-95 transition"
                  >
                    {editingCard ? 'Save Changes' : 'Add Card to Wallet'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* REMINDER SETTINGS MODAL */}
        {isSettingsOpen && (
          <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4">
            <div className="w-full max-w-[440px] rounded-t-3xl sm:rounded-3xl bg-[#0d1424] border border-slate-800 p-5 shadow-2xl space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-cyan-400" />
                  <h3 className="text-base font-bold text-white">Reminder Schedule & Alerts</h3>
                </div>
                <button
                  onClick={() => setIsSettingsOpen(false)}
                  className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-2.5">
                {/* 5 Days Before */}
                <label className="flex items-center justify-between p-3.5 rounded-2xl bg-slate-900/90 border border-slate-800 cursor-pointer">
                  <div>
                    <div className="text-xs font-mono text-cyan-400">5 DAYS BEFORE</div>
                    <div className="text-sm font-semibold text-white mt-0.5">
                      Payment coming up
                    </div>
                  </div>
                  <input
                    type="checkbox"
                    checked={settings.fiveDaysBefore}
                    onChange={(e) =>
                      setSettings((s) => ({ ...s, fiveDaysBefore: e.target.checked }))
                    }
                    className="w-5 h-5 accent-cyan-400 rounded"
                  />
                </label>

                {/* 2 Days Before */}
                <label className="flex items-center justify-between p-3.5 rounded-2xl bg-slate-900/90 border border-slate-800 cursor-pointer">
                  <div>
                    <div className="text-xs font-mono text-amber-400">2 DAYS BEFORE</div>
                    <div className="text-sm font-semibold text-white mt-0.5">
                      Payment still outstanding
                    </div>
                  </div>
                  <input
                    type="checkbox"
                    checked={settings.twoDaysBefore}
                    onChange={(e) =>
                      setSettings((s) => ({ ...s, twoDaysBefore: e.target.checked }))
                    }
                    className="w-5 h-5 accent-cyan-400 rounded"
                  />
                </label>

                {/* Due Date */}
                <label className="flex items-center justify-between p-3.5 rounded-2xl bg-slate-900/90 border border-slate-800 cursor-pointer">
                  <div>
                    <div className="text-xs font-mono text-rose-400">ON DUE DATE</div>
                    <div className="text-sm font-semibold text-white mt-0.5">
                      Final payment reminder
                    </div>
                  </div>
                  <input
                    type="checkbox"
                    checked={settings.onDueDate}
                    onChange={(e) =>
                      setSettings((s) => ({ ...s, onDueDate: e.target.checked }))
                    }
                    className="w-5 h-5 accent-cyan-400 rounded"
                  />
                </label>
              </div>

              {/* System Push & Test Button */}
              <div className="pt-2 border-t border-slate-800 space-y-2.5">
                <div className="flex items-center justify-between p-3 rounded-2xl bg-slate-900/60 border border-slate-800/80">
                  <div className="flex items-center gap-2.5">
                    <Clock className="w-4 h-4 text-cyan-400" />
                    <div>
                      <div className="text-xs font-semibold text-white">
                        Device Push Notifications
                      </div>
                      <div className="text-[11px] text-slate-400">
                        System-level heads-up alerts
                      </div>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={toggleBrowserNotifications}
                    className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                      settings.browserNotifications
                        ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                        : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                    }`}
                  >
                    {settings.browserNotifications ? 'Enabled' : 'Enable'}
                  </button>
                </div>

                <button
                  type="button"
                  onClick={triggerTestReminder}
                  className="w-full py-2.5 rounded-xl bg-cyan-500/15 hover:bg-cyan-500/25 border border-cyan-500/35 text-cyan-300 text-xs font-semibold flex items-center justify-center gap-2 transition"
                >
                  <Sparkles className="w-4 h-4" />
                  Test Reminder Alert
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
