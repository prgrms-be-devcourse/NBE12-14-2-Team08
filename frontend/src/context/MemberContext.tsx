'use client';

import { createContext, useContext, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  clearAuthSession,
  getAuthSession,
  memberApi,
  MemberResponse,
  saveAuthSession,
} from '../lib/memberApi';

interface MemberUser {
  id: string;
  name: string;
  username: string;
}

interface MemberContextValue {
  currentUser: MemberUser | null;
  authReady: boolean;
  login: (username: string, password: string, redirectTo?: string) => Promise<void>;
  signup: (nickname: string, username: string, password: string) => Promise<void>;
  updateProfile: (nickname: string, password?: string) => Promise<void>;
  logout: () => void;
  deleteAccount: () => Promise<void>;
}

const MemberContext = createContext<MemberContextValue | null>(null);

function toUser(member: MemberResponse): MemberUser {
  return {
    id: String(member.memberId),
    name: member.nickname,
    username: member.username,
  };
}

export function MemberProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [currentUser, setCurrentUser] = useState<MemberUser | null>(null);
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    if (!getAuthSession()) {
      setAuthReady(true);
      return;
    }
    memberApi.getMe()
      .then((member) => setCurrentUser(toUser(member)))
      .catch(() => clearAuthSession())
      .finally(() => setAuthReady(true));
  }, []);

  const login = async (username: string, password: string, redirectTo = '/main') => {
    const session = await memberApi.login(username, password);
    saveAuthSession(session);
    try {
      setCurrentUser(toUser(await memberApi.getMe()));
      const safeRedirect = redirectTo.startsWith('/') && !redirectTo.startsWith('//')
        ? redirectTo
        : '/main';
      router.push(safeRedirect);
    } catch (error) {
      clearAuthSession();
      throw error;
    }
  };

  const signup = async (nickname: string, username: string, password: string) => {
    await memberApi.signup(nickname, username, password);
    router.push('/login');
  };

  const updateProfile = async (nickname: string, password?: string) => {
    setCurrentUser(toUser(await memberApi.updateMe(nickname, password)));
  };

  const logout = () => {
    clearAuthSession();
    setCurrentUser(null);
    router.push('/login');
  };

  const deleteAccount = async () => {
    await memberApi.deleteMe();
    clearAuthSession();
    setCurrentUser(null);
    router.push('/login');
  };

  return (
    <MemberContext.Provider value={{ currentUser, authReady, login, signup, updateProfile, logout, deleteAccount }}>
      {children}
    </MemberContext.Provider>
  );
}

export function useMember() {
  const context = useContext(MemberContext);
  if (!context) throw new Error('MemberProvider가 필요합니다.');
  return context;
}
