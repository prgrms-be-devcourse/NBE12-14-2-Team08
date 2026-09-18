'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { User, ChallengeRoom, VerificationItem, Habit } from '../types';
import { INITIAL_USERS, INITIAL_ROOMS, INITIAL_VERIFICATIONS } from '../mockData';
import { authApi, roomApi, habitApi, adminApi, API_BASE_URL } from '../lib/api';
import { toRouteId } from '../lib/routeIds';

export type PageType =
  | 'main'
  | 'room'
  | 'habit-detail'
  | 'mypage'
  | 'admin'
  | 'settlement'
  | 'invite'
  | 'login'
  | 'signup';

interface AppContextType {
  currentUser: User | null;
  users: User[];
  rooms: ChallengeRoom[];
  verifications: VerificationItem[];
  currentPage: PageType;
  currentRoomId: string;
  selectedUserId: string;
  toast: string | null;
  apiBaseUrl: string;
  showToast: (msg: string) => void;
  setCurrentPage: (page: PageType) => void;
  syncPageFromRoute: (page: PageType) => void;
  setCurrentRoomId: (id: string) => void;
  setSelectedUserId: (id: string) => void;
  switchUser: (userId: string) => void;
  login: (username: string, password?: string) => Promise<boolean>;
  signup: (name: string, username: string, password?: string) => Promise<boolean>;
  updateProfile: (name: string) => void;
  deleteAccount: () => Promise<void>;
  createRoom: (roomData: {
    title: string;
    description: string;
    password?: string;
    deadline: string;
    maxMembers: number;
    penaltyText: string;
    initialHabitTitle?: string;
  }) => Promise<string>;
  updateRoom: (roomId: string, data: Partial<ChallengeRoom>) => Promise<void>;
  deleteRoom: (roomId: string) => Promise<void>;
  joinRoom: (roomId: string, habitTitle: string) => Promise<boolean>;
  registerOrUpdateHabit: (
    roomId: string,
    title: string,
    description: string,
    weeklyTargetDays: number
  ) => Promise<void>;
  giveUpHabit: (roomId: string, userId: string) => Promise<void>;
  submitVerification: (data: {
    roomId: string;
    type: 'habit' | 'penalty';
    imageUrl: string;
    content: string;
    date: string;
  }) => Promise<void>;
  approveVerification: (id: string) => Promise<void>;
  rejectVerification: (id: string) => Promise<void>;
  batchApprove: (ids: string[]) => Promise<void>;
  batchReject: (ids: string[]) => Promise<void>;
  resetAllData: () => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const router = useRouter();
  const [users, setUsers] = useState<User[]>(INITIAL_USERS);
  const [currentUser, setCurrentUser] = useState<User | null>(INITIAL_USERS[0]);
  const [rooms, setRooms] = useState<ChallengeRoom[]>(INITIAL_ROOMS);
  const [verifications, setVerifications] = useState<VerificationItem[]>(INITIAL_VERIFICATIONS);

  const [currentPage, setCurrentPageState] = useState<PageType>('main');
  const [currentRoomId, setCurrentRoomId] = useState<string>('room-1');
  const [selectedUserId, setSelectedUserId] = useState<string>('user-1');
  const [toast, setToast] = useState<string | null>(null);

  const getPagePath = (page: PageType) => {
    switch (page) {
      case 'login':
        return '/login';
      case 'signup':
        return '/signup';
      case 'main':
        return '/main';
      case 'room':
      case 'settlement':
        return `/group/${encodeURIComponent(toRouteId(currentRoomId))}`;
      case 'habit-detail': {
        const room = rooms.find((r) => r.id === currentRoomId);
        const member = room?.members.find((m) => m.userId === selectedUserId);
        return `/group/${encodeURIComponent(toRouteId(currentRoomId))}/habit/${encodeURIComponent(
          toRouteId(member?.habit?.id || selectedUserId)
        )}`;
      }
      case 'admin':
        return `/admin/group/${encodeURIComponent(toRouteId(currentRoomId))}/verification`;
      case 'invite':
        return `/invite/${encodeURIComponent(toRouteId(currentRoomId))}`;
      default:
        return null;
    }
  };

  const setCurrentPage = (page: PageType) => {
    setCurrentPageState(page);
    const path = getPagePath(page);
    if (path) router.push(path);
  };

  const syncPageFromRoute = (page: PageType) => {
    setCurrentPageState(page);
  };

  // Load backend rooms on mount if backend is alive
  useEffect(() => {
    async function loadBackendData() {
      const res = await roomApi.getRooms();
      if (res.data && Array.isArray(res.data) && res.data.length > 0) {
        setRooms(res.data);
        console.log('Backend rooms loaded successfully!');
      }
    }
    loadBackendData();
  }, []);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => {
      setToast((prev) => (prev === msg ? null : prev));
    }, 2800);
  };

  const switchUser = (userId: string) => {
    const target = users.find((u) => u.id === userId);
    if (target) {
      setCurrentUser(target);
      showToast(`'${target.name}' 님으로 전환되었습니다.`);
    }
  };

  const login = async (username: string, password = '1234') => {
    // 1. Send real backend request
    await authApi.login(username, password);

    // 2. State update
    const user = users.find((u) => u.username.toLowerCase() === username.toLowerCase());
    if (user) {
      setCurrentUser(user);
      setCurrentPage('main');
      showToast(`'${user.name}' 님, 환영합니다!`);
      return true;
    }
    const newUser: User = {
      id: 'user-' + Date.now(),
      username,
      name: username,
      avatarUrl: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop&q=80',
      penaltyCount: 0,
    };
    setUsers((prev) => [...prev, newUser]);
    setCurrentUser(newUser);
    setCurrentPage('main');
    showToast(`'${newUser.name}' 님으로 로그인되었습니다.`);
    return true;
  };

  const signup = async (name: string, username: string, password = '1234') => {
    // 1. Send real backend request
    await authApi.signup({ name, username, password });

    const newUser: User = {
      id: 'user-' + Date.now(),
      username,
      name,
      avatarUrl: `https://api.dicebear.com/7.x/bottts/svg?seed=${username}`,
      penaltyCount: 0,
    };
    setUsers((prev) => [...prev, newUser]);
    setCurrentUser(newUser);
    setCurrentPage('main');
    showToast(`'${name}' 님, 회원가입이 완료되었습니다!`);
    return true;
  };

  const updateProfile = (name: string) => {
    if (!currentUser) return;
    const updated = { ...currentUser, name };
    setCurrentUser(updated);
    setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
    showToast('프로필이 성공적으로 변경되었습니다.');
  };

  const deleteAccount = async () => {
    if (!currentUser) return;
    await authApi.deleteAccount(currentUser.id);

    const remainingUsers = users.filter((u) => u.id !== currentUser.id);
    setUsers(remainingUsers);
    setCurrentUser(remainingUsers[0] || null);
    setCurrentPage('login');
    showToast('회원 탈퇴가 완료되었습니다.');
  };

  const createRoom = async (data: {
    title: string;
    description: string;
    password?: string;
    deadline: string;
    maxMembers: number;
    penaltyText: string;
    initialHabitTitle?: string;
  }) => {
    if (!currentUser) return '';
    const newRoomId = 'room-' + Date.now();
    const today = new Date().toISOString().split('T')[0];

    // 1. Real backend POST /rooms request
    await roomApi.createRoom({
      ...data,
      initialHabitTitle: data.initialHabitTitle,
    });

    // 2. Update state for immediate UI feedback
    const newRoom: ChallengeRoom = {
      id: newRoomId,
      title: data.title,
      description: data.description,
      password: data.password || '',
      startDate: today,
      deadline: data.deadline || '2026-10-31',
      maxMembers: data.maxMembers || 6,
      penaltyText: data.penaltyText || '커피 쏘기 ☕',
      hostId: currentUser.id,
      members: [
        {
          userId: currentUser.id,
          role: 'host',
          penaltyCount: 0,
          habit: {
            id: 'habit-' + Date.now(),
            title: data.initialHabitTitle || '매일 실천 습관 🌿',
            weeklyTargetDays: 7,
            status: 'active',
          },
        },
      ],
    };

    setRooms((prev) => [newRoom, ...prev]);
    setCurrentRoomId(newRoomId);
    showToast(`새 챌린지 방 '${newRoom.title}' 이 생성되었습니다! (백엔드 전송 완료)`);
    return newRoomId;
  };

  const updateRoom = async (roomId: string, data: Partial<ChallengeRoom>) => {
    await roomApi.updateRoom(roomId, data);
    setRooms((prev) => prev.map((r) => (r.id === roomId ? { ...r, ...data } : r)));
    showToast('챌린지 방 정보가 수정되었습니다.');
  };

  const deleteRoom = async (roomId: string) => {
    await roomApi.deleteRoom(roomId);
    setRooms((prev) => prev.filter((r) => r.id !== roomId));
    setCurrentPage('main');
    showToast('챌린지 방이 삭제되었습니다.');
  };

  const joinRoom = async (roomId: string, habitTitle: string) => {
    if (!currentUser) return false;
    const room = rooms.find((r) => r.id === roomId);
    if (!room) return false;

    if (room.members.some((m) => m.userId === currentUser.id)) {
      showToast('이미 참여 중인 챌린지 방입니다.');
      setCurrentRoomId(roomId);
      setCurrentPage('room');
      return true;
    }

    // Real backend request
    await roomApi.joinRoom(roomId, habitTitle);

    const updatedRooms = rooms.map((r) => {
      if (r.id === roomId) {
        return {
          ...r,
          members: [
            ...r.members,
            {
              userId: currentUser.id,
              role: 'member' as const,
              penaltyCount: 0,
              habit: {
                id: 'habit-' + Date.now(),
                title: habitTitle || '나만의 도전 습관',
                weeklyTargetDays: 7,
                status: 'active' as const,
              },
            },
          ],
        };
      }
      return r;
    });

    setRooms(updatedRooms);
    setCurrentRoomId(roomId);
    setCurrentPage('room');
    showToast(`'${room.title}' 방에 성공적으로 참여했습니다!`);
    return true;
  };

  const registerOrUpdateHabit = async (
    roomId: string,
    title: string,
    description: string,
    weeklyTargetDays: number
  ) => {
    if (!currentUser) return;
    await habitApi.registerOrUpdateHabit(roomId, { title, description, weeklyTargetDays });

    setRooms((prev) =>
      prev.map((r) => {
        if (r.id === roomId) {
          const members = r.members.map((m) => {
            if (m.userId === currentUser.id) {
              return {
                ...m,
                habit: {
                  id: m.habit?.id || 'habit-' + Date.now(),
                  title,
                  description,
                  weeklyTargetDays,
                  status: 'active' as const,
                },
              };
            }
            return m;
          });
          return { ...r, members };
        }
        return r;
      })
    );
    showToast('습관이 등록되었습니다. (백엔드 전송 완료)');
  };

  const giveUpHabit = async (roomId: string, userId: string) => {
    await habitApi.giveUpHabit(roomId, userId);

    setRooms((prev) =>
      prev.map((r) => {
        if (r.id === roomId) {
          const members = r.members.map((m) => {
            if (m.userId === userId) {
              return {
                ...m,
                penaltyCount: m.penaltyCount + 1,
                habit: m.habit ? { ...m.habit, status: 'failed' as const } : undefined,
              };
            }
            return m;
          });
          return { ...r, members };
        }
        return r;
      })
    );
    showToast('습관이 실패로 처리되었습니다. 벌칙을 수행해주세요!');
  };

  const submitVerification = async (data: {
    roomId: string;
    type: 'habit' | 'penalty';
    imageUrl: string;
    content: string;
    date: string;
  }) => {
    if (!currentUser) return;
    const room = rooms.find((r) => r.id === data.roomId);
    const member = room?.members.find((m) => m.userId === currentUser.id);
    const habitTitle = member?.habit?.title || (data.type === 'habit' ? '오늘의 습관' : '벌칙 수행');

    // Real backend request
    await habitApi.submitVerification(data);

    const newVer: VerificationItem = {
      id: 'ver-' + Date.now(),
      roomId: data.roomId,
      userId: currentUser.id,
      userName: currentUser.name,
      userAvatar: currentUser.avatarUrl,
      habitTitle,
      type: data.type,
      date: data.date,
      imageUrl: data.imageUrl,
      content: data.content,
      status: 'pending',
      createdAt: new Date().toLocaleDateString('ko-KR', { hour: '2-digit', minute: '2-digit' }),
    };

    setVerifications((prev) => [newVer, ...prev]);
    showToast(
      data.type === 'habit'
        ? '습관 인증이 제출되었습니다! (백엔드 전송 & 방장 승인 대기)'
        : '벌칙 인증이 제출되었습니다!'
    );
  };

  const approveVerification = async (id: string) => {
    await adminApi.approveVerification(id);
    setVerifications((prev) =>
      prev.map((v) => (v.id === id ? { ...v, status: 'approved' as const } : v))
    );
    showToast('인증이 승인되었습니다. ✅ (백엔드 동기화 완료)');
  };

  const rejectVerification = async (id: string) => {
    await adminApi.rejectVerification(id);
    setVerifications((prev) =>
      prev.map((v) => (v.id === id ? { ...v, status: 'rejected' as const } : v))
    );
    showToast('인증이 거절되었습니다.');
  };

  const batchApprove = async (ids: string[]) => {
    await adminApi.batchApprove(ids);
    setVerifications((prev) =>
      prev.map((v) => (ids.includes(v.id) ? { ...v, status: 'approved' as const } : v))
    );
    showToast(`${ids.length}건의 인증이 일괄 승인되었습니다. (백엔드 처리 완료)`);
  };

  const batchReject = async (ids: string[]) => {
    await adminApi.batchReject(ids);
    setVerifications((prev) =>
      prev.map((v) => (ids.includes(v.id) ? { ...v, status: 'rejected' as const } : v))
    );
    showToast(`${ids.length}건의 인증이 거절되었습니다.`);
  };

  const resetAllData = () => {
    setUsers(INITIAL_USERS);
    setCurrentUser(INITIAL_USERS[0]);
    setRooms(INITIAL_ROOMS);
    setVerifications(INITIAL_VERIFICATIONS);
    setCurrentRoomId('room-1');
    setSelectedUserId('user-1');
    setCurrentPage('main');
    showToast('초기 상태(샘플 방 1개)로 리셋되었습니다.');
  };

  return (
    <AppContext.Provider
      value={{
        currentUser,
        users,
        rooms,
        verifications,
        currentPage,
        currentRoomId,
        selectedUserId,
        toast,
        apiBaseUrl: API_BASE_URL,
        showToast,
        setCurrentPage,
        syncPageFromRoute,
        setCurrentRoomId,
        setSelectedUserId,
        switchUser,
        login,
        signup,
        updateProfile,
        deleteAccount,
        createRoom,
        updateRoom,
        deleteRoom,
        joinRoom,
        registerOrUpdateHabit,
        giveUpHabit,
        submitVerification,
        approveVerification,
        rejectVerification,
        batchApprove,
        batchReject,
        resetAllData,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) throw new Error('useApp must be used within AppProvider');
  return context;
};
