import { apiRequest } from './memberApi';

export type HabitStatus = 'ACTIVE' | 'FAILED';
export type HabitVerifyStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type PenaltyVerifyStatus = 'REQUIRED' | 'PENDING' | 'APPROVED' | 'REJECTED';
export type GroupMemberRole = 'OWNER' | 'MEMBER';

export interface HabitResponse {
  id: number;
  createDate: string;
  modifyDate: string;
  groupMemberId: number;
  title: string;
  description: string | null;
  days: number;
  status: HabitStatus;
}

export interface HabitVerifyResponse {
  id: number;
  habitId: number;
  verifyDate: string;
  status: HabitVerifyStatus;
  description: string | null;
  imageUrl: string | null;
}

export interface GroupMemberDetail {
  groupMemberId: number;
  memberId: number;
  nickname: string;
  role: GroupMemberRole;
  penaltyCount: number;
}

export interface GroupMemberSimple {
  groupMemberId: number;
  memberId: number;
  nickname: string;
  username: string;
  role: GroupMemberRole;
  habitId: number | null;
  habitTitle: string | null;
  habitDescription: string | null;
}

export interface PenaltyVerifySummary {
  id: number;
  habitId: number;
  verifyDate: string | null;
  habitTitle: string;
  memberNickname: string;
  imageUrl: string | null;
  status: PenaltyVerifyStatus;
}

export interface PenaltyVerifyDetail {
  id: number;
  verifyDate: string | null;
  habitTitle: string;
  penaltyText: string;
  description: string | null;
  imageUrl: string | null;
  status: PenaltyVerifyStatus;
}

export interface UploadUrlResponse {
  uploadUrl: string;
  publicUrl: string;
}

export const habitApi = {
  getHabit: (habitId: number | string) =>
    apiRequest<HabitResponse>(`/habits/${habitId}`, {}, true),

  createHabit: (
    groupId: number | string,
    body: { title: string; description: string | null; days: number }
  ) =>
    apiRequest<HabitResponse>(`/groups/${groupId}/habits`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, true),

  failHabit: (habitId: number | string) =>
    apiRequest<void>(`/habits/fail/${habitId}`, { method: 'POST' }, true),

  getGroupMembers: (groupId: number | string) =>
    apiRequest<GroupMemberSimple[]>(`/groups/${groupId}/members`, {}, true),

  getGroupMember: (groupId: number | string, groupMemberId: number | string) =>
    apiRequest<GroupMemberDetail>(`/groups/${groupId}/members/${groupMemberId}`, {}, true),

  getHabitVerifications: (habitId: number | string) =>
    apiRequest<HabitVerifyResponse[]>(`/habits/${habitId}/verifications`, {}, true),

  createHabitVerification: (
    habitId: number | string,
    body: { description: string | null; imageUrl: string | null }
  ) =>
    apiRequest<HabitVerifyResponse>(`/habits/${habitId}/verifications`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, true),

  getHabitUploadUrl: (habitId: number | string, filename: string) =>
    apiRequest<UploadUrlResponse>(`/storage/habits/${habitId}/upload-url`, {
      method: 'POST',
      body: JSON.stringify({ filename }),
    }, true),

  getPenaltiesByGroupMember: (groupId: number | string, groupMemberId: number | string) =>
    apiRequest<PenaltyVerifySummary[]>(
      `/groups/${groupId}/members/${groupMemberId}/penalties`, {}, true
    ),

  getPenaltyDetail: (id: number | string) =>
    apiRequest<PenaltyVerifyDetail>(`/penalties/${id}`, {}, true),

  submitPenalty: (habitId: number | string, body: { description: string | null; imageUrl: string }) =>
    apiRequest<PenaltyVerifyDetail>(`/habits/${habitId}/penalties`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, true),

  resubmitPenalty: (penaltyId: number | string, body: { description: string | null; imageUrl: string }) =>
    apiRequest<PenaltyVerifyDetail>(`/penalties/${penaltyId}/resubmit`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    }, true),

  getPenaltyUploadUrl: (filename: string) =>
    apiRequest<UploadUrlResponse>('/penalties/upload-url', {
      method: 'POST',
      body: JSON.stringify({ filename }),
    }, true),
};

// Supabase 서명 URL로 파일 바이너리를 직접 PUT 업로드한다 (백엔드를 거치지 않음 - 인증 불필요).
export async function uploadFileToSignedUrl(uploadUrl: string, file: File): Promise<void> {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type || 'application/octet-stream' },
    body: file,
  });

  if (!response.ok) {
    throw new Error('이미지 업로드에 실패했습니다.');
  }
}
