import { apiRequest } from './memberApi';

export type GroupStatus = 'ACTIVE' | 'FINISH';

export interface GroupSummary {
  id: number;
  title: string;
  description: string;
  memberLimit: number;
  currentMemberCount: number;
  status: GroupStatus;
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

export const groupApi = {
  getMyGroups: (status: GroupStatus) =>
    apiRequest<GroupSummary[]>(`/groups?status=${status}`, {}, true),
  create: (request: CreateGroupRequest) =>
    apiRequest<GroupDetail>('/groups', {
      method: 'POST',
      body: JSON.stringify(request),
    }, true),
  join: (inviteCode: string, password: string) =>
    apiRequest<void>(`/groups/join/${encodeURIComponent(inviteCode)}`, {
      method: 'POST',
      body: JSON.stringify({ password }),
    }, true),
};
