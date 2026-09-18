/**
 * 백엔드 REST API 통신 클라이언트 모듈
 * 환경변수: NEXT_PUBLIC_API_URL (기본: http://localhost:8080/api)
 * 브라우저 개발자 도구의 Network 탭에서 모든 API 요청/응답을 확인할 수 있습니다.
 */

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<{ data: T | null; error: string | null; status: number }> {
  const url = `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  console.log(`[API Request] ${options.method || 'GET'} ${url}`, options.body || '');

  try {
    const res = await fetch(url, {
      ...options,
      headers,
    });

    const status = res.status;
    let data: T | null = null;
    try {
      data = await res.json();
    } catch {
      data = null;
    }

    if (!res.ok) {
      console.warn(`[API Error ${status}] ${url}`, data);
      return { data: null, error: `서버 오류 (${status})`, status };
    }

    console.log(`[API Response ${status}] ${url}`, data);
    return { data, error: null, status };
  } catch (err: any) {
    console.warn(`[API Network Error] 백엔드 서버(${API_BASE_URL})와 연결되지 않았습니다. (백엔드 미실행 상태)`, err.message);
    return {
      data: null,
      error: `백엔드 서버 연결 불가: ${err.message || '네트워크 오류'}`,
      status: 0,
    };
  }
}

// 1. 인증 관련 API (Auth)
export const authApi = {
  login: (username: string, password?: string) =>
    request<{ token: string; user: any }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  signup: (userData: { name: string; username: string; password?: string }) =>
    request<{ user: any }>('/auth/signup', {
      method: 'POST',
      body: JSON.stringify(userData),
    }),

  deleteAccount: (userId: string) =>
    request('/auth/withdraw', {
      method: 'POST',
      body: JSON.stringify({ userId }),
    }),
};

// 2. 챌린지 방 관련 API (Rooms)
export const roomApi = {
  getRooms: () =>
    request<any[]>('/rooms', {
      method: 'GET',
    }),

  getRoomDetail: (roomId: string) =>
    request<any>(`/rooms/${roomId}`, {
      method: 'GET',
    }),

  createRoom: (roomData: {
    title: string;
    description: string;
    password?: string;
    deadline: string;
    maxMembers: number;
    penaltyText: string;
    initialHabitTitle?: string;
  }) =>
    request<any>('/rooms', {
      method: 'POST',
      body: JSON.stringify(roomData),
    }),

  updateRoom: (roomId: string, data: any) =>
    request<any>(`/rooms/${roomId}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    }),

  deleteRoom: (roomId: string) =>
    request(`/rooms/${roomId}`, {
      method: 'DELETE',
    }),

  joinRoom: (roomId: string, habitTitle: string) =>
    request<any>(`/rooms/${roomId}/join`, {
      method: 'POST',
      body: JSON.stringify({ habitTitle }),
    }),
};

// 3. 습관 및 인증 관련 API (Habits & Verifications)
export const habitApi = {
  registerOrUpdateHabit: (
    roomId: string,
    habitData: { title: string; description?: string; weeklyTargetDays: number }
  ) =>
    request<any>(`/rooms/${roomId}/habits`, {
      method: 'POST',
      body: JSON.stringify(habitData),
    }),

  giveUpHabit: (roomId: string, userId: string) =>
    request(`/rooms/${roomId}/habits/giveup`, {
      method: 'POST',
      body: JSON.stringify({ userId }),
    }),

  submitVerification: (verificationData: {
    roomId: string;
    type: 'habit' | 'penalty';
    imageUrl: string;
    content: string;
    date: string;
  }) =>
    request<any>('/verifications', {
      method: 'POST',
      body: JSON.stringify(verificationData),
    }),

  getVerifications: (roomId: string) =>
    request<any[]>(`/verifications?roomId=${roomId}`, {
      method: 'GET',
    }),
};

// 4. 방장 관리자 API (Admin)
export const adminApi = {
  approveVerification: (id: string) =>
    request(`/admin/verifications/${id}/approve`, {
      method: 'POST',
    }),

  rejectVerification: (id: string) =>
    request(`/admin/verifications/${id}/reject`, {
      method: 'POST',
    }),

  batchApprove: (ids: string[]) =>
    request('/admin/verifications/batch-approve', {
      method: 'POST',
      body: JSON.stringify({ ids }),
    }),

  batchReject: (ids: string[]) =>
    request('/admin/verifications/batch-reject', {
      method: 'POST',
      body: JSON.stringify({ ids }),
    }),
};
