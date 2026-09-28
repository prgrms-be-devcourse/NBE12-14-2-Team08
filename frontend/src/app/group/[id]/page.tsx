'use client';

import React, { useState, useEffect, use } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

interface GroupDetailData {
    id: number;
    title: string;
    description: string;
    startDate: string;
    deadline: string;
    penalty: string;
    inviteCode: string;
    memberLimit: number;
    inviteLink?: string;
}

interface GroupMemberItem {
    id: number; // groupMemberId
    memberId: number;
    nickname: string;
    role: 'OWNER' | 'MEMBER';
    habitId: number | null;
    habitTitle: string | null;
    habitDescription: string | null;
}

export default function GroupDetailPage({
                                            params,
                                        }: {
    params: Promise<{ id: string }> | { id: string };
}) {
    const router = useRouter();
    const resolvedParams = params instanceof Promise ? use(params) : params;
    const rawId = resolvedParams?.id;

    const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

    const [group, setGroup] = useState<GroupDetailData | null>(null);
    const [members, setMembers] = useState<GroupMemberItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [currentRole, setCurrentRole] = useState<'OWNER' | 'MEMBER'>('MEMBER');

    const getAuthHeader = () => {
        const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') || '' : '';
        return token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    };

    // 1. groupId 유효성 검증 및 그룹/멤버 데이터 fetch
    useEffect(() => {
        // 1번 수정: groupId가 없거나 숫자가 아닌 비정상적인 접근 차단 및 리다이렉트
        if (!rawId || isNaN(Number(rawId))) {
            alert('유효하지 않은 그룹 접근입니다.');
            router.replace('/main');
            return;
        }

        const groupId = rawId;

        const fetchGroupData = async () => {
            try {
                setLoading(true);

                // A. 그룹 기본 정보 조회
                const groupRes = await fetch(`${API_BASE_URL}/api/groups/${groupId}`, {
                    headers: { Authorization: getAuthHeader() },
                });

                if (!groupRes.ok) {
                    throw new Error('그룹 정보를 불러오는 데 실패했습니다.');
                }
                const groupData: GroupDetailData = await groupRes.json();
                setGroup(groupData);

                // B. 그룹 멤버 및 습관 목록 조회
                const memberRes = await fetch(`${API_BASE_URL}/api/groups/${groupId}/members`, {
                    headers: { Authorization: getAuthHeader() },
                });

                if (memberRes.ok) {
                    const memberData: GroupMemberItem[] = await memberRes.json();
                    setMembers(memberData);

                    const myNickname = typeof window !== 'undefined' ? localStorage.getItem('nickname') : null;
                    const myMemberId = typeof window !== 'undefined' ? localStorage.getItem('memberId') : null;

                    const me = memberData.find(
                        (m) => m.nickname === myNickname || String(m.memberId) === myMemberId
                    );

                    if (me) {
                        setCurrentRole(me.role);
                    } else if (memberData.length > 0 && memberData[0].role === 'OWNER') {
                        setCurrentRole(memberData[0].role);
                    }
                }
            } catch (err: any) {
                console.error(err);
                alert(err.message || '데이터를 불러오지 못했습니다.');
            } finally {
                setLoading(false);
            }
        };

        fetchGroupData();
    }, [rawId, API_BASE_URL, router]);

    // 초대 링크 / 코드 복사
    const handleCopyInvite = () => {
        const inviteText = group?.inviteLink || group?.inviteCode || '';
        if (!inviteText) return;

        if (navigator.clipboard) {
            navigator.clipboard.writeText(inviteText);
            alert('초대 코드가 클립보드에 복사되었습니다: ' + inviteText);
        } else {
            prompt('초대 코드를 복사하세요:', inviteText);
        }
    };

    // 방 탈퇴 처리
    const handleLeaveGroup = async () => {
        if (currentRole === 'OWNER') {
            alert('방장은 그룹 활성화 상태에서 바로 탈퇴할 수 없습니다.\n습관 상세 페이지에서 권한을 위임한 후 탈퇴해 주세요.');
            return;
        }

        if (!confirm('정말 이 방에서 탈퇴하시겠습니까?')) return;

        try {
            const res = await fetch(`${API_BASE_URL}/api/groups/${rawId}/leave`, {
                method: 'DELETE',
                headers: { Authorization: getAuthHeader() },
            });

            if (!res.ok) {
                const errorMsg = await res.text();
                throw new Error(errorMsg || '방 탈퇴 처리에 실패했습니다.');
            }

            alert('그룹에서 탈퇴했습니다.');
            router.push('/main');
        } catch (err: any) {
            alert(err.message);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-[#FAFCFA] text-sm text-gray-500 font-sans">
                그룹 정보를 불러오는 중...
            </div>
        );
    }

    if (!group) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-[#FAFCFA] text-sm text-gray-500 font-sans">
                그룹 정보를 찾을 수 없습니다.
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-[#FAFCFA] p-6 text-gray-900 font-sans">
            <div className="mx-auto max-w-xl rounded-[40px] bg-white p-8 shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100">

                {/* 네비게이션 헤더 */}
                <div className="flex items-center justify-between text-xs text-gray-400 font-bold mb-4">
                    <span>내기? 내기!</span>
                </div>

                <div className="mb-6">
                    <Link
                        href="/main"
                        className="inline-flex items-center text-xs font-bold text-gray-400 hover:text-black transition"
                    >
                        ← 방 목록으로
                    </Link>
                </div>

                {/* 1. 상단 그룹 메인 정보 카드 */}
                <section className="mb-8 rounded-[32px] border-2 border-black p-6 bg-white shadow-sm">
                    <div className="flex items-start justify-between gap-2 mb-2">
                        <h1 className="text-xl font-black text-gray-900 flex items-center gap-1.5">
                            {group.title}
                            <span className="text-lg">👑</span>
                        </h1>
                        <button
                            onClick={handleLeaveGroup}
                            className="rounded-xl border-2 border-rose-400 bg-rose-50/80 px-3.5 py-1 text-xs font-bold text-rose-600 hover:bg-rose-100 transition cursor-pointer"
                        >
                            방 탈퇴
                        </button>
                    </div>

                    <p className="text-xs font-medium text-gray-500 mb-4">{group.description}</p>

                    <div className="flex items-center justify-between text-xs font-bold text-gray-700 pb-4 border-b border-gray-100">
                        <span>멤버 {members.length} / {group.memberLimit}명</span>
                        <span>{group.deadline ? `${group.deadline}까지` : '상시 진행'}</span>
                    </div>

                    <div className="mt-4 flex items-center gap-1.5 text-xs">
                        <span className="font-bold text-gray-500">벌칙 :</span>
                        <span className="font-extrabold text-rose-700 bg-rose-50 px-2.5 py-0.5 rounded-full border border-rose-100">
              {group.penalty || '벌칙 없음'}
            </span>
                    </div>

                    {/* 버튼 영역 (초대 링크 복사 & 방장 전용 관리 버튼) */}
                    <div className="mt-6 flex flex-wrap items-center justify-between gap-2 pt-2">
                        <button
                            onClick={handleCopyInvite}
                            className="rounded-2xl border-2 border-black bg-white px-4 py-2 text-xs font-bold text-gray-800 hover:bg-gray-50 transition cursor-pointer shadow-sm"
                        >
                            초대 링크 복사
                        </button>

                        {/* 방장(OWNER)일 때만 노출되는 관리 구역 */}
                        {currentRole === 'OWNER' && (
                            <div className="flex items-center gap-2">
                                <button
                                    onClick={() => alert('방 설정 모달 또는 페이지로 이동')}
                                    className="rounded-2xl border-2 border-black bg-white px-4 py-2 text-xs font-bold text-gray-800 hover:bg-gray-50 transition cursor-pointer shadow-sm"
                                >
                                    방 설정
                                </button>
                                <Link
                                    href={`/admin/group/${rawId}/verification`}
                                    className="rounded-2xl border-2 border-black bg-white px-4 py-2 text-xs font-bold text-gray-800 hover:bg-gray-50 transition shadow-sm text-center"
                                >
                                    미인증 내역
                                </Link>
                            </div>
                        )}
                    </div>
                </section>

                {/* 2. 하단 팀원별 습관 목록 */}
                <section>
                    <h2 className="text-sm font-black text-gray-800 mb-3 px-1">습관</h2>

                    <div className="flex flex-col gap-3">
                        {members.map((m, index) => {
                            const hasHabit = !!m.habitId;
                            const linkHref = hasHabit ? `/group/${rawId}/habit/${m.habitId}` : '#';

                            return (
                                <div
                                    key={m.habitId ? `habit-${m.habitId}` : `member-${m.id}-${index}`}
                                    onClick={() => {
                                        if (hasHabit) {
                                            router.push(linkHref);
                                        } else {
                                            alert(`${m.nickname} 님이 등록한 습관이 아직 없습니다.`);
                                        }
                                    }}
                                    className="flex items-center justify-between rounded-[22px] border-2 border-black p-4 bg-white shadow-sm hover:shadow-md hover:-translate-y-0.5 transition cursor-pointer"
                                >
                                    {/* 좌측: 유저 정보 */}
                                    <div className="flex flex-col">
                                        <div className="flex items-center gap-1.5">
                                            <span className="text-sm font-black text-gray-900">{m.nickname}</span>
                                            {m.role === 'OWNER' && <span className="text-xs">👑</span>}
                                        </div>
                                        <span className="text-[11px] font-medium text-gray-400 font-mono">
                      member#{m.memberId}
                    </span>
                                    </div>

                                    {/* 우측: 습관 타이틀 및 설명 */}
                                    <div className="flex items-center gap-3 text-right">
                                        <div>
                                            <div className="text-xs font-black text-gray-800">
                                                {m.habitTitle || '등록된 습관 없음'}
                                            </div>
                                            <div className="text-[10px] text-gray-400 truncate max-w-[150px]">
                                                {m.habitDescription || '습관 설명이 없습니다.'}
                                            </div>
                                        </div>
                                        <span className="text-base font-black text-gray-400">›</span>
                                    </div>
                                </div>
                            );
                        })}

                        {members.length === 0 && (
                            <div className="w-full py-10 text-center text-xs text-gray-400 bg-gray-50 rounded-2xl border border-dashed border-gray-200">
                                참여 중인 멤버가 없습니다.
                            </div>
                        )}
                    </div>
                </section>

            </div>
        </div>
    );
}