'use client';

import React, { useState, useEffect, use, useCallback } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useMember } from '@/context/MemberContext';
import { apiRequest } from '@/lib/memberApi';
import { EditRoomModal } from '@/components/EditRoomModal';

interface GroupDetailData {
    id: number;
    title: string;
    description: string;
    startDate?: string;
    deadline?: string;
    penalty?: string;
    inviteCode?: string;
    memberLimit: number;
    inviteLink?: string;
}

interface GroupMemberItem {
    id: number;
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
    const { currentUser, authReady } = useMember();
    const resolvedParams = params instanceof Promise ? use(params) : params;
    const rawId = resolvedParams?.id;

    const [group, setGroup] = useState<GroupDetailData | null>(null);
    const [members, setMembers] = useState<GroupMemberItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [currentRole, setCurrentRole] = useState<'OWNER' | 'MEMBER'>('MEMBER');
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);

    const fetchGroupData = useCallback(async () => {
        if (!rawId || isNaN(Number(rawId))) return;
        const groupId = Number(rawId);

        try {
            const groupData = await apiRequest<any>(`/groups/${groupId}`, {}, true);
            setGroup({
                id: groupData.id || groupData.groupId || groupId,
                title: groupData.title || '',
                description: groupData.description || '',
                startDate: groupData.startDate,
                deadline: groupData.deadline,
                penalty: groupData.penalty || '',
                inviteCode: groupData.inviteCode || '',
                memberLimit: groupData.memberLimit || 10,
                inviteLink: groupData.inviteLink,
            });

            try {
                const rawMembers = await apiRequest<any[]>(`/groups/${groupId}/members`, {}, true);

                const formattedMembers: GroupMemberItem[] = (rawMembers || []).map((m: any) => ({
                    id: m.groupMemberId || m.id,
                    memberId: m.memberId,
                    nickname: m.nickname || m.memberName || '',
                    role: m.role || 'MEMBER',
                    habitId: m.habitId || null,
                    habitTitle: m.habitTitle || null,
                    habitDescription: m.habitDescription || null,
                }));

                setMembers(formattedMembers);

               if (currentUser) {
            const me = formattedMembers.find(
                (m) => String(m.memberId) === String(currentUser.id)
            );

            setCurrentRole(me?.role ?? 'MEMBER');
        }
    } catch (memberErr: any) {
        console.error('멤버 목록 조회 실패:', memberErr);
    }
        } catch (err: any) {
            console.error('그룹 정보 조회 에러:', err);
            alert(err.message || '그룹 정보를 불러오지 못했습니다.');
        } finally {
            setLoading(false);
        }
    }, [rawId, currentUser]);

    useEffect(() => {
        if (!authReady) return;
        if (!currentUser) {
            router.replace('/login');
            return;
        }

        if (!rawId || isNaN(Number(rawId))) {
            alert('유효하지 않은 그룹 접근입니다.');
            router.replace('/main');
            return;
        }

        setLoading(true);
        fetchGroupData();
    }, [rawId, authReady, currentUser, router, fetchGroupData]);

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

    const handleLeaveGroup = async () => {
        if (currentRole === 'OWNER') {
            alert('방장은 그룹 활성화 상태에서 바로 탈퇴할 수 없습니다.\n습관 상세 페이지에서 권한을 위임한 후 탈퇴해 주세요.');
            return;
        }

        if (!confirm('정말 이 방에서 탈퇴하시겠습니까?')) return;

        try {
            await apiRequest<void>(`/groups/${rawId}/leave`, { method: 'DELETE' }, true);
            alert('그룹에서 탈퇴했습니다.');
            router.push('/main');
        } catch (err: any) {
            alert(err.message || '방 탈퇴 처리에 실패했습니다.');
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

                <section className="mb-8 rounded-[32px] border-2 border-black p-6 bg-white shadow-sm">
                    <div className="flex items-start justify-between gap-2 mb-2">
                        <h1 className="text-xl font-black text-gray-900 flex items-center gap-1.5">
                            {group.title}
                            {currentRole === 'OWNER' && (
                                <span
                                    className="text-lg"
                                    role="img"
                                    aria-label="방장"
                                    title="방장"
                                >
                                    👑
                                </span>
                            )}
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

                    <div className="mt-6 flex flex-wrap items-center justify-between gap-2 pt-2">
                        <button
                            onClick={handleCopyInvite}
                            className="rounded-2xl border-2 border-black bg-white px-4 py-2 text-xs font-bold text-gray-800 hover:bg-gray-50 transition cursor-pointer shadow-sm"
                        >
                            초대 링크 복사
                        </button>

                        {currentRole === 'OWNER' && (
                            <div className="flex items-center gap-2">
                                <button
                                    onClick={() => setIsEditModalOpen(true)}
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

                <section>
                    <h2 className="text-sm font-black text-gray-800 mb-3 px-1">습관</h2>

                    <div className="flex flex-col gap-3">
                        {members.map((m, index) => {
                            const linkHref = `/group/${rawId}/member/${m.memberId}`;

                            return (
                                <div
                                    key={`member-${m.id}-${index}`}
                                    onClick={() => router.push(linkHref)}
                                    className="flex items-center justify-between rounded-[22px] border-2 border-black p-4 bg-white shadow-sm hover:shadow-md hover:-translate-y-0.5 transition cursor-pointer"
                                >
                                    <div className="flex flex-col">
                                        <div className="flex items-center gap-1.5">
                                            <span className="text-sm font-black text-gray-900">{m.nickname}</span>
                                            {m.role === 'OWNER' && <span className="text-xs">👑</span>}
                                        </div>
                                        <span className="text-[11px] font-medium text-gray-400 font-mono">
                                            member#{m.memberId}
                                        </span>
                                    </div>

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

            {group && (
                <EditRoomModal
                    isOpen={isEditModalOpen}
                    onClose={() => setIsEditModalOpen(false)}
                    groupId={group.id}
                    initialData={{
                        title: group.title,
                        description: group.description,
                        startDate: group.startDate || new Date().toISOString().slice(0, 10),
                        deadline: group.deadline || '',
                        memberLimit: group.memberLimit,
                        penalty: group.penalty || '',
                    }}
                    onSuccess={fetchGroupData}
                />
            )}
        </div>
    );
}
