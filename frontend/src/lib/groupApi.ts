import { apiRequest } from './memberApi';

export type GroupStatus = 'ACTIVE' | 'FINISH';
export type GroupMemberRole = 'OWNER' | 'MEMBER';

export interface GroupSummary {
  id: number;
  title: string;
  description: string;
  memberLimit: number;
  currentMemberCount: number;
  status: GroupStatus;
  role: GroupMemberRole;
}

export interface CreateGroupRequest {
  title: string;
  description: string;
  deadline: string;
  penalty: string;
  password: string;
  memberLimit: number;
}

interface GroupDetail {
  id: number;
}

export interface JoinGroupResponse {
  groupId: number;
}

export interface GroupInvitePreview {
  id: number;
  title: string;
  description: string | null;
  startDate: string;
  deadline: string | null;
  penalty: string | null;
  memberLimit: number;
  currentMemberCount: number;
  status: GroupStatus;
}

export const groupApi = {
  getMyGroups: (status: GroupStatus) =>
    apiRequest<GroupSummary[]>(`/groups?status=${status}`, {}, true),
  create: (request: CreateGroupRequest) =>
    apiRequest<GroupDetail>('/groups', {
      method: 'POST',
      body: JSON.stringify(request),
    }, true),
  getInvitePreview: (inviteCode: string) =>
    apiRequest<GroupInvitePreview>(`/groups/invite/${encodeURIComponent(inviteCode)}`),
  join: (inviteCode: string, password: string) =>
    apiRequest<JoinGroupResponse>(`/groups/join/${encodeURIComponent(inviteCode)}`, {
      method: 'POST',
      body: JSON.stringify({ password }),
    }, true),
};
