export interface User {
  id: string;
  username: string;
  name: string;
  avatarUrl: string;
  penaltyCount: number;
}

export interface Habit {
  id: string;
  title: string;
  description?: string;
  weeklyTargetDays: number;
  status: 'active' | 'failed' | 'completed';
}

export interface RoomMember {
  userId: string;
  role: 'host' | 'member';
  habit?: Habit;
  penaltyCount: number;
}

export interface ChallengeRoom {
  id: string;
  title: string;
  description?: string;
  password?: string;
  startDate: string;
  deadline: string;
  maxMembers: number;
  penaltyText: string;
  hostId: string;
  members: RoomMember[];
  isClosed?: boolean;
}

export interface VerificationItem {
  id: string;
  roomId: string;
  userId: string;
  userName: string;
  userAvatar: string;
  habitTitle: string;
  type: 'habit' | 'penalty';
  date: string;
  imageUrl: string;
  content: string;
  status: 'pending' | 'approved' | 'rejected';
  createdAt: string;
}

export interface CalendarDayStatus {
  date: string; // YYYY-MM-DD
  dayNum: number;
  status: 'success' | 'failed' | 'none' | 'future';
  verification?: VerificationItem;
  penaltyVerification?: VerificationItem;
}
