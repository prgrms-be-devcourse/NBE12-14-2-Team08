"use client";

import React, { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import {
  Calendar,
  Users,
  Copy,
  Check,
  Settings,
  ShieldAlert,
  ArrowLeft,
  ChevronRight,
  LogOut,
  AlertCircle,
  RefreshCw,
  X,
  Coffee,
  Sparkles,
} from "lucide-react";

// =================================================================
// 1. DTO 인터페이스 정의 (Java Record/Entity 매핑 규격)
// =================================================================

export interface GroupDetail {
  id: number;
  title: string;
  description: string;
  startDate: string;
  deadline: string;
  penalty: string;
  inviteCode: string;
  memberLimit: number;
  createDate: string;
  inviteLink: string;
}

export interface GroupMemberSimple {
  id: number;
  memberId: number;
  nickname: string;
  username: string;
  role: "OWNER" | "MEMBER";
  habitId: number | null;
  habitTitle: string | null;
  habitDescription: string | null;
}

// 백엔드 스프링 부트 기본 URL
const BASE_URL = "http://localhost:8080";

export default function GroupDashboardPage() {
  const params = useParams();
  const router = useRouter();

  // Next.js App Router 동적 파라미터 [id] 추출
  const rawId = params?.id;
  const groupId = Array.isArray(rawId) ? rawId[0] : (rawId as string) || "1";

  // 상태 관리
  const [group, setGroup] = useState<GroupDetail | null>(null);
  const [members, setMembers] = useState<GroupMemberSimple[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // 모달 및 인터랙션 상태
  const [isSettingsOpen, setIsSettingsOpen] = useState<boolean>(false);
  const [isLeaveModalOpen, setIsLeaveModalOpen] = useState<boolean>(false);
  const [isLeaving, setIsLeaving] = useState<boolean>(false);
  const [copied, setCopied] = useState<boolean>(false);

  // =================================================================
  // 2. 백엔드 API 연동 함수 (JWT Authorization 실시간 첨부)
  // =================================================================

  const fetchGroupData = useCallback(async () => {
    if (!groupId) return;

    setLoading(true);
    setError(null);

    // [JWT 인증] 로컬 스토리지에서 실제 로그인 토큰 취득
    const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") : null;

    const headers: Record<string, string> = {
      "Content-Type": "application/json",
    };

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    try {
      // 1) 방 정보 조회: GET http://localhost:8080/api/groups/{id}
      const groupRes = await fetch(`${BASE_URL}/api/groups/${groupId}`, {
        method: "GET",
        headers,
      });

      if (!groupRes.ok) {
        if (groupRes.status === 403) {
          throw new Error(
            "접근 권한이 없습니다 (403 Forbidden). localStorage의 'accessToken'이 올바르게 등록되어 있는지 확인해 주세요."
          );
        } else if (groupRes.status === 404) {
          throw new Error(`ID ${groupId}번 방을 찾을 수 없습니다 (404 Not Found).`);
        } else {
          throw new Error(`방 정보 조회 실패 (HTTP 상태 코드: ${groupRes.status})`);
        }
      }

      const groupData: GroupDetail = await groupRes.json();
      setGroup(groupData);

      // 2) 참여 멤버 목록 조회: GET http://localhost:8080/api/groups/{id}/members
      const membersRes = await fetch(`${BASE_URL}/api/groups/${groupId}/members`, {
        method: "GET",
        headers,
      });

      if (!membersRes.ok) {
        if (membersRes.status === 403) {
          throw new Error("멤버 목록 접근 권한이 없습니다 (403 Forbidden).");
        }
        throw new Error(`멤버 목록 조회 실패 (HTTP 상태 코드: ${membersRes.status})`);
      }

      const membersData: GroupMemberSimple[] = await membersRes.json();
      setMembers(membersData);
    } catch (err: any) {
      console.error("[API 연동 오류]", err);
      setError(
        err.message ||
          "스프링 부트 서버(http://localhost:8080)와 연결할 수 없습니다. 서버 실행 여부와 CORS 설정을 확인해 주세요."
      );
    } finally {
      setLoading(false);
    }
  }, [groupId]);

  useEffect(() => {
    fetchGroupData();
  }, [fetchGroupData]);

  // =================================================================
  // 3. 방 탈퇴 API 호출 (DELETE http://localhost:8080/api/groups/{id}/leave)
  // =================================================================

  const handleLeaveGroup = async () => {
    if (!groupId) return;

    const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") : null;

    if (!token) {
      alert("로그인 토큰이 존재하지 않습니다. 먼저 로그인해 주세요.");
      return;
    }

    setIsLeaving(true);
    try {
      const response = await fetch(`${BASE_URL}/api/groups/${groupId}/leave`, {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        if (response.status === 403) {
          throw new Error("방 탈퇴 권한이 없습니다 (403 Forbidden).");
        }
        throw new Error(`방 탈퇴 처리 실패 (HTTP 상태 코드: ${response.status})`);
      }

      alert("방에서 성공적으로 탈퇴 처리되었습니다.");
      setIsLeaveModalOpen(false);

      // 성공 시 방 목록 페이지('/main')로 리다이렉트
      router.push("/main");
    } catch (err: any) {
      console.error("[방 탈퇴 실패]", err);
      alert(err.message || "방 탈퇴 처리 중 오류가 발생했습니다.");
    } finally {
      setIsLeaving(false);
    }
  };

  // 초대 링크 복사 처리
  const handleCopyInviteLink = async () => {
    const link =
      group?.inviteLink ||
      (typeof window !== "undefined"
        ? `${window.location.origin}/group/${groupId}?invite=${group?.inviteCode || ""}`
        : "");

    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(link);
      } else {
        const textarea = document.createElement("textarea");
        textarea.value = link;
        textarea.style.position = "fixed";
        textarea.style.opacity = "0";
        document.body.appendChild(textarea);
        textarea.focus();
        textarea.select();
        document.execCommand("copy");
        document.body.removeChild(textarea);
      }
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error("클립보드 복사 실패:", err);
      alert("초대 링크 복사에 실패했습니다.");
    }
  };

  // 미인증 내역 버튼 클릭 시 외부/관리자 페이지 이동
  const handleNavigateToVerification = () => {
    if (!groupId) return;
    window.location.href = `http://localhost:3000/admin/group/${groupId}/verification`;
  };

  // 4. 로딩 화면
  if (loading) {
    return (
      <main className="min-h-screen bg-[#f8f9fa] py-8 px-4 sm:px-6 lg:px-8 antialiased">
        <div className="max-w-4xl mx-auto space-y-4">
          <div className="h-6 w-24 bg-slate-200/70 rounded-md animate-pulse" />
          <div className="bg-white rounded-2xl md:rounded-3xl border border-slate-200/80 p-6 md:p-10 shadow-xs space-y-8 animate-pulse">
            <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-6 pb-2 border-b border-slate-100">
              <div className="space-y-3 flex-1">
                <div className="h-8 w-64 bg-slate-200 rounded-lg" />
                <div className="h-4 w-96 bg-slate-100 rounded-md" />
              </div>
              <div className="space-y-2 flex flex-col items-start md:items-end">
                <div className="h-7 w-20 bg-rose-100 rounded-md" />
                <div className="h-4 w-36 bg-slate-200 rounded-md" />
              </div>
            </div>
            <div className="h-12 bg-slate-100 rounded-xl" />
            <div className="space-y-3 pt-2">
              <div className="h-6 w-32 bg-slate-200 rounded-md" />
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-20 bg-slate-100/80 rounded-2xl border border-slate-200/60" />
              ))}
            </div>
          </div>
        </div>
      </main>
    );
  }

  // 5. 에러 화면
  if (error || !group) {
    return (
      <main className="min-h-screen bg-[#f8f9fa] py-12 px-4 sm:px-6 lg:px-8 flex items-center justify-center antialiased">
        <div className="w-full max-w-lg bg-white border border-slate-200/90 rounded-2xl md:rounded-3xl p-8 md:p-10 shadow-sm text-center">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-rose-50 text-rose-500 mb-5 border border-rose-100">
            <AlertCircle className="w-7 h-7" />
          </div>
          <h2 className="text-xl md:text-2xl font-bold text-slate-900 mb-2">백엔드 서버 연동 오류</h2>
          <p className="text-sm text-slate-600 mb-4">
            스프링 부트 API 서버(<code>${BASE_URL}</code>)와 통신 중 문제가 발생했습니다.
          </p>
          <div className="text-left bg-rose-50/70 border border-rose-200/80 rounded-xl p-4 mb-6 text-xs text-rose-800 space-y-1.5 font-mono">
            <p className="font-bold">오류 메시지:</p>
            <p className="break-all font-sans text-rose-700">${error || "알 수 없는 오류"}</p>
          </div>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            <button
              type="button"
              onClick={fetchGroupData}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-slate-900 text-white text-xs font-semibold hover:bg-slate-800 transition-colors"
            >
              <RefreshCw className="w-4 h-4" /> 다시 시도
            </button>
            <Link
              href="/group"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-700 text-xs font-semibold hover:bg-slate-50 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" /> 방 목록으로
            </Link>
          </div>
        </div>
      </main>
    );
  }

  // 6. 메인 UI
  return (
    <main className="min-h-screen bg-[#f8f9fa] text-slate-800 py-8 px-4 sm:px-6 lg:px-8 antialiased">
      <div className="max-w-4xl mx-auto space-y-4">
        {/* 방 목록으로 */}
        <div className="flex items-center justify-between">
          <Link
            href="/main"
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-600 hover:text-slate-900 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>방 목록으로</span>
          </Link>
        </div>

        {/* 메인 대시보드 카드 */}
        <div className="bg-white rounded-2xl md:rounded-3xl border border-slate-200/80 p-6 md:p-10 shadow-xs space-y-8">
          
          {/* 타이틀 및 메타 정보 */}
          <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-6 pb-2 border-b border-slate-100">
            <div className="space-y-2 flex-1">
              <h1 className="text-2xl md:text-3xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
                <span>{group.title}</span>
              </h1>
              <p className="text-slate-500 text-sm md:text-base leading-relaxed">
                {group.description || "등록된 설명이 없습니다."}
              </p>
            </div>

            <div className="flex flex-col items-start md:items-end gap-3 shrink-0">
              <button
                type="button"
                onClick={() => setIsLeaveModalOpen(true)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-rose-200 bg-white text-rose-600 hover:bg-rose-50 text-xs font-semibold transition-colors"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>방 탈퇴</span>
              </button>

              <div className="flex flex-col items-start md:items-end gap-1.5 text-sm text-slate-600 font-medium">
                <div className="inline-flex items-center gap-1.5">
                  <Calendar className="w-4 h-4 text-slate-400" />
                  <span>종료일: {group.deadline || "미정"}까지</span>
                </div>
                <div className="inline-flex items-center gap-1.5">
                  <Users className="w-4 h-4 text-slate-400" />
                  <span>멤버 {members.length} / {group.memberLimit}명</span>
                </div>
              </div>
            </div>
          </div>

          {/* 벌칙 규칙 및 액션 영역 */}
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pt-1">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-2xl bg-rose-50 flex items-center justify-center text-rose-500 shrink-0 border border-rose-100">
                <Coffee className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs font-semibold text-slate-400 tracking-wider">벌칙 규칙</p>
                <p className="text-base md:text-lg font-bold text-rose-600">{group.penalty || "벌칙 없음"}</p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={handleCopyInviteLink}
                className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-sm font-semibold transition-colors shadow-2xs"
              >
                {copied ? <Check className="w-4 h-4 text-emerald-500" /> : <Copy className="w-4 h-4 text-slate-500" />}
                <span>{copied ? "복사 완료!" : "초대 링크 복사"}</span>
              </button>

              <div className="flex items-center gap-2 p-1.5 rounded-2xl border-2 border-dashed border-amber-300/90 bg-amber-50/50">
                <span className="text-xs font-bold text-amber-800 px-2 py-0.5">👑 방장 전용</span>
                <button
                  type="button"
                  onClick={() => setIsSettingsOpen(true)}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 text-xs font-semibold transition-colors shadow-2xs"
                >
                  <Settings className="w-3.5 h-3.5 text-slate-500" />
                  <span>방 설정</span>
                </button>
                <button
                  type="button"
                  onClick={handleNavigateToVerification}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-amber-500 hover:bg-amber-600 text-white text-xs font-semibold transition-colors shadow-2xs"
                >
                  <ShieldAlert className="w-3.5 h-3.5" />
                  <span>미인증 내역</span>
                </button>
              </div>
            </div>
          </div>

          {/* 습관 챌린저 목록 */}
          <div className="space-y-4 pt-2">
            <div className="flex items-center gap-2.5">
              <h2 className="text-lg font-bold text-slate-900">습관 챌린저</h2>
              <span className="px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-600 text-xs font-semibold">
                {members.length}명
              </span>
            </div>
            <p className="text-xs text-slate-400 -mt-2">
              멤버 카드를 누르면 개인별 습관 캘린더와 인증 내역을 확인할 수 있어요.
            </p>

            <div className="space-y-3">
              {members.length === 0 ? (
                <div className="text-center py-10 bg-slate-50 rounded-2xl border border-slate-100">
                  <p className="text-sm text-slate-400">참여 중인 멤버가 없습니다.</p>
                </div>
              ) : (
                members.map((member) => {
                  const isOwner = member.role === "OWNER";
                  return (
                    <div
                      key={member.id}
                      className={`group relative flex items-center justify-between p-4 md:p-5 rounded-2xl bg-white border transition-all ${
                        isOwner ? "border-emerald-300/80 shadow-2xs hover:border-emerald-400" : "border-slate-200/80 hover:border-slate-300"
                      }`}
                    >
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center gap-2">
                          <span className="text-base font-bold text-slate-900">{member.nickname}</span>
                          {isOwner && (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-amber-100 text-amber-800 text-[11px] font-bold">
                              방장
                            </span>
                          )}
                        </div>
                        <span className="text-xs text-slate-400 font-medium">@{member.username}</span>
                      </div>

                      <div className="flex items-center gap-3">
                        <div className="text-right">
                          <p className="text-sm md:text-base font-bold text-slate-800 flex items-center justify-end gap-1.5">
                            <span>{member.habitTitle || "등록된 습관 없음"}</span>
                            <Sparkles className="w-4 h-4 text-emerald-500 inline-block" />
                          </p>
                          {member.habitDescription && (
                            <p className="text-xs text-slate-400 max-w-[200px] truncate mt-0.5">
                              {member.habitDescription}
                            </p>
                          )}
                        </div>
                        <div className="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center text-slate-400 group-hover:bg-slate-200 group-hover:text-slate-600 transition-colors shrink-0">
                          <ChevronRight className="w-4 h-4" />
                        </div>
                      </div>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 방 설정 모달 */}
      {isSettingsOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <Settings className="w-5 h-5 text-slate-700" />
                <h3 className="text-base font-bold text-slate-900">방 설정</h3>
              </div>
              <button
                type="button"
                onClick={() => setIsSettingsOpen(false)}
                className="w-8 h-8 rounded-lg flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-6 space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">방 제목</label>
                <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-800 font-medium">
                  {group.title}
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">방 소개</label>
                <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-800">
                  {group.description || "등록된 설명이 없습니다."}
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1">마감 기한</label>
                  <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-700 font-medium">
                    {group.deadline || "-"}
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-500 mb-1">참여 정원</label>
                  <div className="p-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-700 font-medium">
                    최대 {group.memberLimit}명
                  </div>
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">초대 코드</label>
                <input
                  type="text"
                  readOnly
                  value={group.inviteCode || ""}
                  className="w-full p-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-800 font-mono text-sm"
                />
              </div>
            </div>
            <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end">
              <button
                type="button"
                onClick={() => setIsSettingsOpen(false)}
                className="px-4 py-2 rounded-xl bg-slate-900 text-white text-xs font-semibold hover:bg-slate-800"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 방 탈퇴 확인 모달 */}
      {isLeaveModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="w-full max-w-sm bg-white rounded-2xl shadow-xl border border-rose-100 p-6 space-y-4">
            <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-500 flex items-center justify-center mx-auto border border-rose-100">
              <LogOut className="w-6 h-6" />
            </div>
            <div className="text-center space-y-1.5">
              <h3 className="text-lg font-bold text-slate-900">방에서 탈퇴하시겠습니까?</h3>
              <p className="text-xs text-slate-500">
                탈퇴 시 진행 중인 습관 내역 및 그룹 통계에서 제외되며 복구할 수 없습니다.
              </p>
            </div>
            <div className="flex items-center gap-2 pt-2">
              <button
                type="button"
                disabled={isLeaving}
                onClick={() => setIsLeaveModalOpen(false)}
                className="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-700 text-xs font-semibold hover:bg-slate-50"
              >
                취소
              </button>
              <button
                type="button"
                disabled={isLeaving}
                onClick={handleLeaveGroup}
                className="flex-1 py-2.5 rounded-xl bg-rose-600 text-white text-xs font-semibold hover:bg-rose-700 disabled:opacity-50"
              >
                {isLeaving ? "탈퇴 처리 중..." : "탈퇴하기"}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}