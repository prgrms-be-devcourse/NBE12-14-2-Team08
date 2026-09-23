// 백엔드(Spring Boot) API 호출을 한 곳에 모아둔 파일.
// 이 프로젝트의 모든 fetch 호출은 이 파일을 거쳐서 이루어진다.

export const API_BASE_URL = "http://localhost:8080";

const TOKEN_KEY = "accessToken";

export function getAccessToken(): string | null {
    if (typeof window === "undefined") return null;
    return window.localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string) {
    if (typeof window === "undefined") return;
    window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearAccessToken() {
    if (typeof window === "undefined") return;
    window.localStorage.removeItem(TOKEN_KEY);
}

export class ApiError extends Error {
    status: number;

    constructor(status: number, message: string) {
        super(message);
        this.status = status;
    }
}

async function apiFetch<T>(
    path: string,
    options: RequestInit = {}
): Promise<T> {
    const token = getAccessToken();

    const headers: HeadersInit = {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
    };

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers,
        cache: "no-store",
    });

    // 본문이 없는 응답(204, 또는 Void 반환 엔드포인트의 200)이 섞여 있어
    // 우선 텍스트로 받은 뒤 내용이 있을 때만 JSON으로 파싱한다.
    const text = await response.text();

    if (!response.ok) {
        let message = `요청에 실패했습니다. (${response.status})`;
        if (text) {
            try {
                const body = JSON.parse(text);
                if (body?.message) message = body.message;
            } catch {
                // 응답 본문이 JSON이 아닌 경우 기본 메시지 사용
            }
        }
        throw new ApiError(response.status, message);
    }

    if (!text) {
        return undefined as T;
    }

    return JSON.parse(text) as T;
}

// ---------- 타입 ----------

export type HabitStatus = "ACTIVE" | "FAILED";
export type HabitVerifyStatus = "PENDING" | "APPROVED" | "REJECTED";
export type PenaltyVerifyStatus = "REQUIRED" | "PENDING" | "APPROVED" | "REJECTED";
export type GroupMemberRole = "OWNER" | "MEMBER";

export interface MemberResponse {
    memberId: number;
    nickname: string;
    username: string;
    createDate: string;
    modifyDate: string;
}

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

export interface PenaltyVerifySummary {
    id: number;
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

// ---------- 회원 ----------

export function getMe(): Promise<MemberResponse> {
    return apiFetch<MemberResponse>("/api/members/me");
}

// ---------- 습관 ----------

export function getHabit(habitId: number | string): Promise<HabitResponse> {
    return apiFetch<HabitResponse>(`/api/habits/${habitId}`);
}

export function failHabit(habitId: number | string): Promise<void> {
    return apiFetch<void>(`/api/habits/fail/${habitId}`, { method: "POST" });
}

// ---------- 그룹 멤버 ----------

export function getGroupMember(
    groupId: number | string,
    groupMemberId: number | string
): Promise<GroupMemberDetail> {
    return apiFetch<GroupMemberDetail>(
        `/api/groups/${groupId}/members/${groupMemberId}`
    );
}

// ---------- 습관 인증 ----------

export function getHabitVerifications(
    habitId: number | string
): Promise<HabitVerifyResponse[]> {
    return apiFetch<HabitVerifyResponse[]>(`/api/habits/${habitId}/verifications`);
}

export function createHabitVerification(
    habitId: number | string,
    body: { description: string | null; imageUrl: string | null }
): Promise<HabitVerifyResponse> {
    return apiFetch<HabitVerifyResponse>(`/api/habits/${habitId}/verifications`, {
        method: "POST",
        body: JSON.stringify(body),
    });
}

export function getHabitUploadUrl(
    habitId: number | string,
    filename: string
): Promise<UploadUrlResponse> {
    return apiFetch<UploadUrlResponse>(`/api/storage/habits/${habitId}/upload-url`, {
        method: "POST",
        body: JSON.stringify({ filename }),
    });
}

// ---------- 벌칙 인증 ----------

export function getPenaltiesByGroupMember(
    groupId: number | string,
    groupMemberId: number | string
): Promise<PenaltyVerifySummary[]> {
    return apiFetch<PenaltyVerifySummary[]>(
        `/api/groups/${groupId}/members/${groupMemberId}/penalties`
    );
}

export function getPenaltyDetail(id: number | string): Promise<PenaltyVerifyDetail> {
    return apiFetch<PenaltyVerifyDetail>(`/api/penalties/${id}`);
}

export function submitPenalty(
    habitId: number | string,
    body: { description: string | null; imageUrl: string }
): Promise<PenaltyVerifyDetail> {
    return apiFetch<PenaltyVerifyDetail>(`/api/habits/${habitId}/penalties`, {
        method: "POST",
        body: JSON.stringify(body),
    });
}

export function getPenaltyUploadUrl(filename: string): Promise<UploadUrlResponse> {
    return apiFetch<UploadUrlResponse>("/api/penalties/upload-url", {
        method: "POST",
        body: JSON.stringify({ filename }),
    });
}

// ---------- 파일 업로드 ----------

// Supabase 서명 URL로 파일 바이너리를 직접 PUT 업로드한다 (백엔드를 거치지 않음).
export async function uploadFileToSignedUrl(
    uploadUrl: string,
    file: File
): Promise<void> {
    const response = await fetch(uploadUrl, {
        method: "PUT",
        headers: { "Content-Type": file.type || "application/octet-stream" },
        body: file,
    });

    if (!response.ok) {
        throw new ApiError(response.status, "이미지 업로드에 실패했습니다.");
    }
}
