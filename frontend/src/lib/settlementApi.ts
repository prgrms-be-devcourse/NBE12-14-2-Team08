import { apiRequest } from './memberApi';

export interface SettlementPersonalStats {
  memberId: number;
  nickname: string;
  approvedCount: number;
  totalVerifyCount: number;
  rejectedCount: number;
  penaltyCount: number;
}

export interface SettlementWinner {
  memberId: number;
  nickname: string;
  value: number;
}

export interface SettlementGroupComparison {
  mostDescriptionChars: SettlementWinner | null;
  mostDeadlineVerifications: SettlementWinner | null;
  mostRejectedVerifications: SettlementWinner | null;
  longestPenaltyDelay: SettlementWinner | null;
}

export interface SettlementResponse {
  groupId: number;
  groupTitle: string;
  personalStats: SettlementPersonalStats;
  groupComparison: SettlementGroupComparison;
}

export const settlementApi = {
  getSettlement: (groupId: number | string) =>
    apiRequest<SettlementResponse>(`/groups/${groupId}/settlement`, {}, true),
};
