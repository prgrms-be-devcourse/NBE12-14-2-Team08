'use client';

import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import {
  X,
  Upload,
  Calendar,
  Users,
  Lock,
  Flame,
  AlertTriangle,
  Coffee,
  CheckCircle,
  HelpCircle,
  Sparkles,
  Trash2,
} from 'lucide-react';
import { VerificationItem, ChallengeRoom } from '../types';

// Preset sample photos for quick testing
const HABIT_PRESET_IMAGES = [
  { label: '물 한 잔', url: 'https://images.unsplash.com/photo-1548839140-29a749e1bc4e?w=600&auto=format&fit=crop&q=80' },
  { label: '아침 기상/시계', url: 'https://images.unsplash.com/photo-1563861826100-9cb868fdbe1c?w=600&auto=format&fit=crop&q=80' },
  { label: '이불 정리', url: 'https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?w=600&auto=format&fit=crop&q=80' },
  { label: '양치질', url: 'https://images.unsplash.com/photo-1559599101-f09722fb4948?w=600&auto=format&fit=crop&q=80' },
  { label: '세수/세안', url: 'https://images.unsplash.com/photo-1507652313519-d4e9174996dd?w=600&auto=format&fit=crop&q=80' },
  { label: '독서', url: 'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80' },
  { label: '운동/헬스', url: 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=600&auto=format&fit=crop&q=80' },
];

const PENALTY_PRESET_IMAGES = [
  { label: '스타벅스 커피', url: 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600&auto=format&fit=crop&q=80' },
  { label: '메가커피 테이크아웃', url: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=600&auto=format&fit=crop&q=80' },
  { label: '기프티콘 결제 인증', url: 'https://images.unsplash.com/photo-1556742049-0a67c5574f73?w=600&auto=format&fit=crop&q=80' },
  { label: '디저트 쏘기', url: 'https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=600&auto=format&fit=crop&q=80' },
];

// --- 1. 새 챌린지 방 만들기 모달 ---
export const CreateRoomModal: React.FC<{ isOpen: boolean; onClose: () => void }> = ({ isOpen, onClose }) => {
  const { createRoom, setCurrentPage } = useApp();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [password, setPassword] = useState('');
  const [deadline, setDeadline] = useState('2026-10-08');
  const [maxMembers, setMaxMembers] = useState(6);
  const [penaltyText, setPenaltyText] = useState('커피 사기 ☕');
  const [habitTitle, setHabitTitle] = useState('아침 6시 기상 & 물 한 잔');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    createRoom({
      title,
      description,
      password,
      deadline,
      maxMembers,
      penaltyText,
      initialHabitTitle: habitTitle,
    });
    setCurrentPage('room');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-lg rounded-3xl shadow-2xl border border-slate-100 p-6 md:p-8 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between pb-4 border-b border-slate-100">
          <div>
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <span>✨ 새 챌린지 방 만들기</span>
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">친구와 함께할 습관 챌린지 방을 만들어보세요.</p>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">
              방 이름 <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="예: 매일 아침 6시 기상 챌린지"
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">설명 (선택)</label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="이 방에서 어떤 챌린지를 할지 소개해 주세요."
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent resize-none"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">비밀번호 (선택)</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="비밀번호를 입력해주세요"
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">유효 기한</label>
              <input
                type="date"
                required
                value={deadline}
                onChange={(e) => setDeadline(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">최대 인원 설정</label>
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  min={2}
                  max={20}
                  value={maxMembers}
                  onChange={(e) => setMaxMembers(Number(e.target.value))}
                  className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                />
                <span className="text-xs text-slate-500 shrink-0">명</span>
              </div>
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">
                벌칙 <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={penaltyText}
                onChange={(e) => setPenaltyText(e.target.value)}
                placeholder="예: 커피 사기, 1만원 내기"
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">방장의 첫 습관</label>
            <input
              type="text"
              value={habitTitle}
              onChange={(e) => setHabitTitle(e.target.value)}
              placeholder="예: 기상 후 물 한 잔"
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
            />
          </div>

          <div className="flex items-center justify-end gap-2.5 pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-100 transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl text-sm font-bold text-white bg-emerald-500 hover:bg-emerald-600 shadow-md shadow-emerald-500/20 transition-all hover:scale-102"
            >
              방 만들기
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

// --- 2. 방 설정 모달 (방장 전용) ---
export const RoomSettingModal: React.FC<{
  isOpen: boolean;
  onClose: () => void;
  room: ChallengeRoom;
}> = ({ isOpen, onClose, room }) => {
  const { updateRoom, deleteRoom } = useApp();
  const [title, setTitle] = useState(room.title);
  const [description, setDescription] = useState(room.description || '');
  const [password, setPassword] = useState(room.password || '');
  const [deadline, setDeadline] = useState(room.deadline);
  const [maxMembers, setMaxMembers] = useState(room.maxMembers);
  const [penaltyText, setPenaltyText] = useState(room.penaltyText);

  if (!isOpen) return null;

  const handleUpdate = (e: React.FormEvent) => {
    e.preventDefault();
    updateRoom(room.id, {
      title,
      description,
      password,
      deadline,
      maxMembers,
      penaltyText,
    });
    onClose();
  };

  const handleDelete = () => {
    if (window.confirm('정말 이 챌린지 방을 삭제하시겠습니까? 참여자들의 모든 기록이 사라집니다.')) {
      deleteRoom(room.id);
      onClose();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-lg rounded-3xl shadow-2xl border border-slate-100 p-6 md:p-8 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between pb-4 border-b border-slate-100">
          <div>
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <span>⚙️ 방 설정</span>
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">방 이름과 설정을 수정할 수 있어요.</p>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleUpdate} className="mt-5 space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">방 이름</label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">설명 (선택)</label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent resize-none"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">비밀번호 변경</label>
              <input
                type="text"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="비밀번호를 입력해주세요"
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">목표 기한 변경</label>
              <input
                type="date"
                required
                value={deadline}
                onChange={(e) => setDeadline(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">최대 인원 변경</label>
              <input
                type="number"
                min={room.members.length}
                max={30}
                value={maxMembers}
                onChange={(e) => setMaxMembers(Number(e.target.value))}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 mb-1">벌칙 수정</label>
              <input
                type="text"
                required
                value={penaltyText}
                onChange={(e) => setPenaltyText(e.target.value)}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
              />
            </div>
          </div>

          <div className="flex items-center justify-between pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={handleDelete}
              className="px-4 py-2.5 rounded-xl text-xs font-bold text-rose-600 bg-rose-50 hover:bg-rose-100 flex items-center gap-1.5 transition-colors"
            >
              <Trash2 className="w-4 h-4" />
              <span>방 삭제</span>
            </button>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2.5 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-100 transition-colors"
              >
                취소
              </button>
              <button
                type="submit"
                className="px-6 py-2.5 rounded-xl text-sm font-bold text-white bg-emerald-500 hover:bg-emerald-600 shadow-md shadow-emerald-500/20 transition-all hover:scale-102"
              >
                수정하기
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

// --- 3. 새 습관 등록 모달 ---
export const HabitRegisterModal: React.FC<{
  isOpen: boolean;
  onClose: () => void;
  roomId: string;
  initialTitle?: string;
}> = ({ isOpen, onClose, roomId, initialTitle = '' }) => {
  const { registerOrUpdateHabit } = useApp();
  const [title, setTitle] = useState(initialTitle);
  const [description, setDescription] = useState('');
  const [weeklyDays, setWeeklyDays] = useState(7);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    registerOrUpdateHabit(roomId, title, description, weeklyDays);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-md rounded-3xl shadow-2xl border border-slate-100 p-6">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div>
            <h2 className="text-lg font-bold text-slate-900">🌱 새 습관 등록</h2>
            <p className="text-xs text-slate-500 mt-0.5">이 방에서 함께 실천할 습관을 등록하세요.</p>
          </div>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-slate-100 text-slate-400">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">
              습관 이름 <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="예: 하루 10분 독서하기, 기상 후 물 한 잔"
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">설명 (선택)</label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="실천 방법이나 규칙을 적어주세요."
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent resize-none"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">목표 일수</label>
            <div className="flex items-center gap-2">
              <select
                value={weeklyDays}
                onChange={(e) => setWeeklyDays(Number(e.target.value))}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-transparent bg-white"
              >
                {[1, 2, 3, 4, 5, 6, 7].map((n) => (
                  <option key={n} value={n}>
                    일주일에 {n}일 실천
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-100"
            >
              취소
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl text-sm font-bold text-white bg-emerald-500 hover:bg-emerald-600 shadow-sm"
            >
              등록하기
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

// --- 4. 오늘 인증하기 모달 ---
export const CertifyModal: React.FC<{
  isOpen: boolean;
  onClose: () => void;
  roomId: string;
}> = ({ isOpen, onClose, roomId }) => {
  const { submitVerification } = useApp();
  const todayStr = new Date().toISOString().split('T')[0];

  const [date, setDate] = useState(todayStr);
  const [content, setContent] = useState('');
  const [imageUrl, setImageUrl] = useState(HABIT_PRESET_IMAGES[0].url);

  if (!isOpen) return null;

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        if (typeof reader.result === 'string') {
          setImageUrl(reader.result);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    submitVerification({
      roomId,
      type: 'habit',
      imageUrl,
      content: content || '오늘도 약속한 습관을 성공적으로 실천했습니다!',
      date,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-md rounded-3xl shadow-2xl border border-slate-100 p-6 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div>
            <h2 className="text-lg font-bold text-slate-900 flex items-center gap-1.5">
              <span>📸 오늘 인증하기</span>
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">오늘 실천한 내용을 사진과 함께 남겨보세요.</p>
          </div>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-slate-100 text-slate-400">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          {/* Image Preview & Presets */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">인증 사진</label>
            <div className="relative w-full h-48 rounded-2xl overflow-hidden bg-slate-100 border border-slate-200 flex items-center justify-center group">
              <img src={imageUrl} alt="인증 미리보기" className="w-full h-full object-cover" />
              <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <label className="cursor-pointer px-3.5 py-1.5 bg-white/90 hover:bg-white text-slate-800 text-xs font-bold rounded-lg shadow-md flex items-center gap-1.5">
                  <Upload className="w-3.5 h-3.5" />
                  <span>내 사진 업로드</span>
                  <input type="file" accept="image/*" onChange={handleFileUpload} className="hidden" />
                </label>
              </div>
            </div>

            {/* Quick Sample Presets */}
            <div className="mt-2">
              <span className="text-[11px] text-slate-400 font-medium">빠른 샘플 선택: </span>
              <div className="flex flex-wrap gap-1.5 mt-1">
                {HABIT_PRESET_IMAGES.map((preset, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setImageUrl(preset.url)}
                    className={`text-[11px] px-2 py-0.8 rounded-md border transition-all ${
                      imageUrl === preset.url
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-300 font-bold'
                        : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'
                    }`}
                  >
                    {preset.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">실천 날짜</label>
            <input
              type="date"
              required
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="w-full px-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">인증 내용</label>
            <textarea
              rows={3}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="오늘 어떻게 실천했는지 적어주세요."
              className="w-full px-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-100"
            >
              취소
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl text-sm font-bold text-white bg-emerald-500 hover:bg-emerald-600 shadow-sm"
            >
              인증 등록
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

// --- 5. 벌칙 인증 모달 ---
export const PenaltyCertModal: React.FC<{
  isOpen: boolean;
  onClose: () => void;
  roomId: string;
  penaltyText: string;
}> = ({ isOpen, onClose, roomId, penaltyText }) => {
  const { submitVerification } = useApp();
  const todayStr = new Date().toISOString().split('T')[0];

  const [date, setDate] = useState(todayStr);
  const [content, setContent] = useState('');
  const [imageUrl, setImageUrl] = useState(PENALTY_PRESET_IMAGES[0].url);

  if (!isOpen) return null;

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        if (typeof reader.result === 'string') {
          setImageUrl(reader.result);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    submitVerification({
      roomId,
      type: 'penalty',
      imageUrl,
      content: content || `약속된 벌칙 [${penaltyText}] 을 성실히 수행했습니다!`,
      date,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-md rounded-3xl shadow-2xl border border-rose-100 p-6 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between pb-3 border-b border-rose-100">
          <div>
            <h2 className="text-lg font-bold text-rose-600 flex items-center gap-1.5">
              <span>☕ 벌칙 인증하기</span>
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">벌칙 내용을 남겨보세요. (벌칙: {penaltyText})</p>
          </div>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-slate-100 text-slate-400">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">벌칙 수행 사진</label>
            <div className="relative w-full h-48 rounded-2xl overflow-hidden bg-slate-100 border border-rose-200 flex items-center justify-center group">
              <img src={imageUrl} alt="벌칙 인증 미리보기" className="w-full h-full object-cover" />
              <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                <label className="cursor-pointer px-3.5 py-1.5 bg-white/90 hover:bg-white text-slate-800 text-xs font-bold rounded-lg shadow-md flex items-center gap-1.5">
                  <Upload className="w-3.5 h-3.5" />
                  <span>내 사진 업로드</span>
                  <input type="file" accept="image/*" onChange={handleFileUpload} className="hidden" />
                </label>
              </div>
            </div>

            <div className="mt-2">
              <span className="text-[11px] text-slate-400 font-medium">빠른 벌칙 샘플: </span>
              <div className="flex flex-wrap gap-1.5 mt-1">
                {PENALTY_PRESET_IMAGES.map((preset, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setImageUrl(preset.url)}
                    className={`text-[11px] px-2 py-0.8 rounded-md border transition-all ${
                      imageUrl === preset.url
                        ? 'bg-rose-50 text-rose-700 border-rose-300 font-bold'
                        : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'
                    }`}
                  >
                    {preset.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">인증 날짜</label>
            <input
              type="date"
              required
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="w-full px-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-rose-400"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">인증 내용</label>
            <textarea
              rows={3}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="어떻게 벌칙을 수행했는지 적어주세요."
              className="w-full px-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-rose-400 resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-100"
            >
              취소
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl text-sm font-bold text-white bg-rose-500 hover:bg-rose-600 shadow-sm"
            >
              벌칙 인증 등록
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

// --- 6. 성공한 습관 인증 상세 모달 ---
export const CertDetailModal: React.FC<{
  item: VerificationItem | null;
  onClose: () => void;
}> = ({ item, onClose }) => {
  if (!item) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-sm rounded-3xl shadow-2xl border border-emerald-100 p-6">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div className="flex items-center gap-1.5">
            <span className="text-emerald-600 font-extrabold text-base flex items-center gap-1">
              <CheckCircle className="w-5 h-5 fill-emerald-100 text-emerald-600" />
              성공 인증
            </span>
          </div>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-slate-100 text-slate-400">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="mt-3">
          <p className="text-xs font-bold text-slate-500">{item.habitTitle}</p>
          <div className="mt-2.5 w-full h-56 rounded-2xl overflow-hidden bg-slate-100 border border-slate-200 shadow-inner">
            <img src={item.imageUrl} alt="인증 사진" className="w-full h-full object-cover" />
          </div>
          <div className="mt-3 bg-slate-50 p-3 rounded-xl border border-slate-100">
            <p className="text-xs font-semibold text-slate-800 leading-relaxed">{item.content}</p>
          </div>
          <div className="mt-3 flex items-center justify-between text-[11px] text-slate-400">
            <span>실천자: {item.userName}</span>
            <span>인증 일자: {item.date}</span>
          </div>
        </div>

        <div className="mt-4 pt-3 border-t border-slate-100 flex justify-end">
          <button
            onClick={onClose}
            className="w-full py-2.5 bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold rounded-xl shadow-xs"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  );
};

// --- 7. 벌칙 기록 상세 모달 (실패 상세) ---
export const PenaltyDetailModal: React.FC<{
  item: VerificationItem | null;
  onClose: () => void;
}> = ({ item, onClose }) => {
  if (!item) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-sm rounded-3xl shadow-2xl border border-rose-100 p-6">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <div className="flex items-center gap-1.5">
            <span className="text-rose-600 font-extrabold text-base flex items-center gap-1">
              <Coffee className="w-5 h-5 text-rose-600" />
              벌칙 수행 기록
            </span>
          </div>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-slate-100 text-slate-400">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="mt-3">
          <p className="text-xs font-bold text-slate-500">{item.habitTitle}</p>
          <div className="mt-2.5 w-full h-56 rounded-2xl overflow-hidden bg-slate-100 border border-rose-200 shadow-inner">
            <img src={item.imageUrl} alt="벌칙 사진" className="w-full h-full object-cover" />
          </div>
          <div className="mt-3 bg-rose-50/50 p-3 rounded-xl border border-rose-100">
            <p className="text-xs font-semibold text-rose-900 leading-relaxed">{item.content}</p>
          </div>
          <div className="mt-3 flex items-center justify-between text-[11px] text-slate-400">
            <span>수행자: {item.userName}</span>
            <span>인증 일자: {item.date}</span>
          </div>
        </div>

        <div className="mt-4 pt-3 border-t border-slate-100 flex justify-end">
          <button
            onClick={onClose}
            className="w-full py-2.5 bg-rose-500 hover:bg-rose-600 text-white text-xs font-bold rounded-xl shadow-xs"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  );
};

// --- 8. 습관 실패/포기 모달 ---
export const HabitFailModal: React.FC<{
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
}> = ({ isOpen, onClose, onConfirm }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-sm rounded-3xl shadow-2xl border border-rose-200 p-6 text-center">
        <div className="w-12 h-12 rounded-full bg-rose-100 text-rose-500 flex items-center justify-center mx-auto mb-3">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <h3 className="text-base font-bold text-slate-900">습관 실패 확정</h3>
        <p className="text-xs text-slate-500 mt-2 leading-relaxed">
          이 습관을 실패로 종료할까요?<br />
          <span className="text-rose-500 font-semibold">(벌칙수행의 기록이 추가됩니다.)</span>
        </p>

        <div className="mt-6 flex items-center justify-center gap-2">
          <button
            onClick={onClose}
            className="px-4 py-2.5 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors"
          >
            취소
          </button>
          <button
            onClick={() => {
              onConfirm();
              onClose();
            }}
            className="px-4 py-2.5 rounded-xl text-xs font-bold text-white bg-rose-500 hover:bg-rose-600 shadow-md shadow-rose-500/20 transition-colors"
          >
            실패 확정 후 벌칙 수행하기
          </button>
        </div>
      </div>
    </div>
  );
};

// --- 9. 회원탈퇴 모달 ---
export const DeleteAccountModal: React.FC<{
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
}> = ({ isOpen, onClose, onConfirm }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-sm rounded-3xl shadow-2xl border border-rose-200 p-6 text-center">
        <div className="w-12 h-12 rounded-full bg-rose-100 text-rose-500 flex items-center justify-center mx-auto mb-3">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <div className="text-sm font-extrabold text-slate-900">내기? 내기!</div>
        <h3 className="text-base font-bold text-rose-600 mt-1">회원탈퇴를 진행하시겠습니까?</h3>
        <p className="text-xs text-slate-500 mt-2 leading-relaxed">
          회원탈퇴를 하게되면 이후에 회원님의<br />
          <span className="font-semibold text-slate-700">소중한 데이터 복구는 어렵습니다.</span>
        </p>

        <div className="mt-6 flex items-center justify-center gap-2">
          <button
            onClick={() => {
              onConfirm();
              onClose();
            }}
            className="px-4 py-2.5 rounded-xl text-xs font-bold text-rose-600 bg-rose-50 hover:bg-rose-100 transition-colors"
          >
            평생 탈퇴
          </button>
          <button
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-emerald-500 hover:bg-emerald-600 shadow-sm transition-colors"
          >
            돌아가기
          </button>
        </div>
      </div>
    </div>
  );
};

// --- 10. 방 입장 비밀번호 확인 모달 ---
export const RoomPasswordModal: React.FC<{
  isOpen: boolean;
  onClose: () => void;
  room: ChallengeRoom;
  onSuccess: () => void;
}> = ({ isOpen, onClose, room, onSuccess }) => {
  const [inputPw, setInputPw] = useState('');
  const [error, setError] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!room.password || inputPw === room.password) {
      onSuccess();
      onClose();
    } else {
      setError(true);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-sm rounded-3xl shadow-2xl border border-slate-100 p-6">
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
          <h2 className="text-base font-bold text-slate-900">인증 비밀번호 입력</h2>
          <button onClick={onClose} className="p-1 rounded-full hover:bg-slate-100 text-slate-400">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div className="text-center py-2 bg-slate-50 rounded-2xl border border-slate-100">
            <p className="text-sm font-bold text-slate-800">{room.title}</p>
            <p className="text-xs text-slate-500 mt-0.5">{room.description}</p>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1">인증 비밀번호</label>
            <input
              type="password"
              autoFocus
              value={inputPw}
              onChange={(e) => {
                setInputPw(e.target.value);
                setError(false);
              }}
              placeholder="비밀번호를 입력하세요 (기본: 1234)"
              className="w-full px-4 py-2.5 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
            {error && <p className="text-xs text-rose-500 mt-1 font-semibold">비밀번호가 일치하지 않습니다.</p>}
          </div>

          <div className="pt-2 flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100"
            >
              취소
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl text-xs font-bold text-white bg-emerald-500 hover:bg-emerald-600 shadow-sm"
            >
              인증하기
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
