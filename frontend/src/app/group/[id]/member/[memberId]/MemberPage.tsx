"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
    ArrowLeft,
    Calendar as CalendarIcon,
    CheckCircle2,
    Camera,
    Coffee,
    Crown,
    AlertTriangle,
    Upload,
    Plus,
} from "lucide-react";
import { useMember } from "@/context/MemberContext";
import { ModalShell, ModalHeader, ModalActions } from "@/components/ModalKit";
import {
    habitApi,
    uploadFileToSignedUrl,
    type HabitResponse,
    type HabitVerifyResponse,
    type GroupMemberDetail,
    type PenaltyVerifySummary,
    type PenaltyVerifyDetail,
} from "@/lib/habitApi";

type Props = { groupId: string; memberId: string };

export function MemberPage({ groupId, memberId }: Props) {
    const router = useRouter();
    const { currentUser, authReady } = useMember();

    const [member, setMember] = useState<GroupMemberDetail | null>(null);
    const [penalties, setPenalties] = useState<PenaltyVerifySummary[]>([]);
    const [habit, setHabit] = useState<HabitResponse | null>(null);
    const [verifies, setVerifies] = useState<HabitVerifyResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<Error | null>(null);

    const [activePenalty, setActivePenalty] = useState<PenaltyVerifySummary | null>(null);
    const [penaltyDetail, setPenaltyDetail] = useState<PenaltyVerifyDetail | null>(null);
    const [showCertifyModal, setShowCertifyModal] = useState(false);
    const [showFailModal, setShowFailModal] = useState(false);
    const [showCreateHabitModal, setShowCreateHabitModal] = useState(false);
    const [certDetail, setCertDetail] = useState<HabitVerifyResponse | null>(null);

    const load = useCallback(async () => {
        try {
            setError(null);

            // 백엔드는 groupMemberId 기준으로 조회하므로, 라우트의 memberId를 그룹 멤버 목록에서 찾아 변환한다.
            const groupMembers = await habitApi.getGroupMembers(groupId);
            const target = groupMembers.find((m) => String(m.memberId) === String(memberId));
            if (!target) {
                throw new Error("멤버 정보를 찾을 수 없습니다.");
            }
            const groupMemberId = target.groupMemberId;

            const [memberRes, penaltiesRes] = await Promise.all([
                habitApi.getGroupMember(groupId, groupMemberId),
                habitApi.getPenaltiesByGroupMember(groupId, groupMemberId),
            ]);
            setMember(memberRes);
            setPenalties(penaltiesRes);

            const isOwnPage = currentUser?.id === String(memberRes.memberId);
            if (isOwnPage) {
                try {
                    const habitRes = await habitApi.getActiveHabit(groupId);
                    setHabit(habitRes);
                    setVerifies(await habitApi.getHabitVerifications(habitRes.id));
                } catch {
                    setHabit(null);
                    setVerifies([]);
                }
            } else {
                setHabit(null);
                setVerifies([]);
            }
        } catch (err) {
            setError(err instanceof Error ? err : new Error("알 수 없는 오류가 발생했습니다."));
        } finally {
            setLoading(false);
        }
    }, [groupId, memberId, currentUser]);

    useEffect(() => {
        if (!authReady) return;
        if (!currentUser) {
            router.replace(`/login?redirect=/group/${groupId}/member/${memberId}`);
            return;
        }
        load();
    }, [authReady, currentUser, load, router, groupId, memberId]);

    if (!authReady || !currentUser) {
        return <CenteredMessage text="로그인 정보를 확인하는 중..." />;
    }

    if (loading) {
        return <CenteredMessage text="불러오는 중..." />;
    }

    if (error) {
        return <CenteredMessage text={error.message} />;
    }

    if (!member) {
        return <CenteredMessage text="멤버 정보를 찾을 수 없습니다." />;
    }

    const isMe = currentUser.id === String(member.memberId);
    const isHost = member.role === "OWNER";

    // 아직 제출 전(REQUIRED)이라 날짜가 없는 벌칙은 맨 위로 오도록 미래 날짜 취급
    const rows = [...penalties].sort((a, b) =>
        (b.verifyDate ?? "9999-99-99").localeCompare(a.verifyDate ?? "9999-99-99")
    );

    return (
        <div className="max-w-4xl mx-auto px-4 sm:px-6 py-6 md:py-8">
            {/* Top Nav */}
            <div className="flex items-center justify-between mb-5">
                <Link
                    href={`/group/${groupId}`}
                    className="flex items-center gap-1.5 text-xs font-bold text-slate-500 hover:text-slate-900 transition-colors"
                >
                    <ArrowLeft className="w-4 h-4" />
                    <span>챌린지 방으로</span>
                </Link>
                <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">
                    멤버 상세 페이지
                </span>
            </div>

            {/* 프로필 & 누적 벌칙 & 도전 습관 */}
            <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 mb-6">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div className="flex items-center gap-3.5">
                        <div className="relative">
                            <div className="w-14 h-14 rounded-full bg-emerald-100 border-2 border-emerald-400 flex items-center justify-center text-lg font-black text-emerald-700">
                                {member.nickname.slice(0, 1)}
                            </div>
                            {isHost && (
                                <div className="absolute -bottom-1 -right-1 bg-amber-400 text-white p-1 rounded-full shadow-2xs">
                                    <Crown className="w-3 h-3 fill-white" />
                                </div>
                            )}
                        </div>
                        <div>
                            <div className="flex items-center gap-2">
                                <h1 className="text-lg font-black text-slate-900">{member.nickname}</h1>
                                {isHost && (
                                    <span className="text-[10px] font-bold px-1.5 py-0.5 bg-amber-100 text-amber-700 rounded-md">
                                        방장
                                    </span>
                                )}
                                {isMe && (
                                    <span className="text-[10px] font-bold px-1.5 py-0.5 bg-emerald-100 text-emerald-700 rounded-md">
                                        나
                                    </span>
                                )}
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center gap-3 bg-rose-50/80 border border-rose-100 px-4 py-2.5 rounded-2xl shrink-0">
                        <Coffee className="w-4 h-4 text-rose-500" />
                        <div>
                            <span className="text-[10px] font-bold text-rose-400 uppercase">누적 벌칙</span>
                            <p className="text-sm font-black text-rose-600">벌칙 횟수 : {member.penaltyCount}회</p>
                        </div>
                    </div>
                </div>

                {/* 도전 습관 & 액션 버튼 (본인 페이지에서만 표시) */}
                {isMe && habit && (
                    <div className="mt-5 pt-5 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                        <div>
                            <span className="text-[11px] font-bold text-emerald-600 uppercase">도전 습관</span>
                            <div className="flex items-center gap-2">
                                <h2 className="text-lg font-extrabold text-slate-900">{habit.title}</h2>
                                <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 border border-emerald-100">
                                    주 {habit.days}회
                                </span>
                            </div>
                            <p className="text-xs text-slate-500 mt-0.5">
                                {habit.description || "매일매일 실천하고 성공을 기록해보세요!"}
                            </p>
                        </div>

                        {habit.status === "ACTIVE" && (
                            <div className="flex items-center gap-2 shrink-0">
                                <button
                                    onClick={() => setShowCertifyModal(true)}
                                    className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl font-bold text-xs text-white bg-emerald-500 hover:bg-emerald-600 shadow-md shadow-emerald-500/20 transition-all cursor-pointer"
                                >
                                    <Camera className="w-4 h-4" />
                                    <span>오늘 인증하기</span>
                                </button>
                                <button
                                    onClick={() => setShowFailModal(true)}
                                    className="px-3.5 py-2.5 rounded-xl font-bold text-xs text-rose-600 hover:bg-rose-50 border border-rose-200 transition-colors cursor-pointer"
                                >
                                    습관 포기하기
                                </button>
                            </div>
                        )}
                        {habit.status === "FAILED" && (
                            <span className="text-xs font-bold px-3 py-1.5 rounded-xl bg-slate-100 text-slate-500 shrink-0">
                                실패 처리된 습관입니다
                            </span>
                        )}
                    </div>
                )}

                {isMe && !habit && (
                    <div className="mt-5 pt-5 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                        <div>
                            <span className="text-[11px] font-bold text-emerald-600 uppercase">도전 습관</span>
                            <p className="text-sm text-slate-500 mt-0.5">아직 진행 중인 습관이 없습니다.</p>
                        </div>
                        <button
                            onClick={() => setShowCreateHabitModal(true)}
                            className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl font-bold text-xs text-white bg-emerald-500 hover:bg-emerald-600 shadow-md shadow-emerald-500/20 transition-all cursor-pointer shrink-0"
                        >
                            <Plus className="w-4 h-4" />
                            <span>습관을 추가해주세요</span>
                        </button>
                    </div>
                )}
            </div>

            {/* 인증 캘린더 (본인 페이지에서만 표시) */}
            {isMe && habit && <VerifyCalendar verifies={verifies} onSelectDay={setCertDetail} />}

            {/* 벌칙 기록 (실패한 습관 종류와 무관하게 이 멤버의 전체 벌칙) */}
            <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6">
                <h3 className="text-sm font-extrabold text-slate-900 mb-4 flex items-center justify-between">
                    <span>벌칙 기록</span>
                    <span className="text-xs font-normal text-slate-400">최근 벌칙 타임라인</span>
                </h3>

                {rows.length === 0 ? (
                    <p className="text-xs text-slate-400">아직 기록이 없습니다.</p>
                ) : (
                    <div className="space-y-3">
                        {rows.map((penalty) => (
                            <div
                                key={penalty.id}
                                className="p-3.5 sm:p-4 rounded-2xl border border-slate-100 bg-slate-50/60 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                            >
                                <div>
                                    <div className="flex items-center gap-2 mb-1">
                                        <span className="text-xs font-bold text-slate-500">
                                            {penalty.verifyDate ?? "미제출"}
                                        </span>
                                        <span className="text-xs font-extrabold text-slate-900">{penalty.habitTitle}</span>
                                    </div>
                                    <p className="text-[11px] text-slate-400">벌칙 대상 기록입니다.</p>
                                </div>
                                <div className="flex items-center gap-2 shrink-0">
                                    <span className="text-xs font-bold text-rose-600 px-2.5 py-1 rounded-xl bg-rose-100">
                                        미인증
                                    </span>
                                    {penalty.status === "REQUIRED" ? (
                                        isMe && (
                                            <button
                                                onClick={() => setActivePenalty(penalty)}
                                                className="flex items-center gap-1 text-xs font-bold text-white bg-rose-500 hover:bg-rose-600 px-3 py-1.5 rounded-xl shadow-xs transition-all cursor-pointer"
                                            >
                                                <Coffee className="w-3.5 h-3.5" />
                                                <span>벌칙 수행하기</span>
                                            </button>
                                        )
                                    ) : (
                                        <button
                                            onClick={async () => {
                                                try {
                                                    const detail = await habitApi.getPenaltyDetail(penalty.id);
                                                    setPenaltyDetail(detail);
                                                } catch {
                                                    // 상세 조회 실패 시 조용히 무시 (요약 정보는 이미 화면에 있음)
                                                }
                                            }}
                                            className="flex items-center gap-1 text-xs font-bold text-rose-700 bg-rose-100 hover:bg-rose-200 px-3 py-1.5 rounded-xl transition-colors cursor-pointer"
                                        >
                                            <Coffee className="w-3.5 h-3.5" />
                                            <span>벌칙완료 확인</span>
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* 모달들 */}
            {showCertifyModal && habit && (
                <CertifyModal
                    onClose={() => setShowCertifyModal(false)}
                    onSubmit={async (file, description) => {
                        let imageUrl: string | null = null;
                        if (file) {
                            const { uploadUrl, publicUrl } = await habitApi.getHabitUploadUrl(habit.id, file.name);
                            await uploadFileToSignedUrl(uploadUrl, file);
                            imageUrl = publicUrl;
                        }
                        await habitApi.createHabitVerification(habit.id, { description: description || null, imageUrl });
                        setShowCertifyModal(false);
                        await load();
                    }}
                />
            )}

            {showFailModal && habit && (
                <HabitFailModal
                    onClose={() => setShowFailModal(false)}
                    onConfirm={async () => {
                        await habitApi.failHabit(habit.id);
                        setShowFailModal(false);
                        await load();
                    }}
                />
            )}

            {showCreateHabitModal && (
                <CreateHabitModal
                    onClose={() => setShowCreateHabitModal(false)}
                    onSubmit={async (title, description, days) => {
                        await habitApi.createHabit(groupId, { title, description: description || null, days });
                        setShowCreateHabitModal(false);
                        await load();
                    }}
                />
            )}

            {certDetail && <CertDetailModal item={certDetail} onClose={() => setCertDetail(null)} />}

            {activePenalty && (
                <PenaltyCertModal
                    onClose={() => setActivePenalty(null)}
                    onSubmit={async (file, description) => {
                        const { uploadUrl, publicUrl } = await habitApi.getPenaltyUploadUrl(file.name);
                        await uploadFileToSignedUrl(uploadUrl, file);
                        await habitApi.submitPenalty(activePenalty.habitId, {
                            description: description || null,
                            imageUrl: publicUrl,
                        });
                        setActivePenalty(null);
                        await load();
                    }}
                />
            )}

            {penaltyDetail && (
                <PenaltyDetailModal item={penaltyDetail} onClose={() => setPenaltyDetail(null)} />
            )}
        </div>
    );
}

function CenteredMessage({ text }: { text: string }) {
    return (
        <div className="max-w-4xl mx-auto px-4 py-16 text-center space-y-3">
            <p className="text-slate-500">{text}</p>
        </div>
    );
}

// ---------- 캘린더 ----------

function VerifyCalendar({
    verifies,
    onSelectDay,
}: {
    verifies: HabitVerifyResponse[];
    onSelectDay: (v: HabitVerifyResponse) => void;
}) {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth(); // 0-indexed
    const today = now.getDate();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const startDayOfWeek = new Date(year, month, 1).getDay();
    const weekDays = ["일", "월", "화", "수", "목", "금", "토"];

    // 상태와 무관하게 그 날짜에 제출된 인증이 있으면 그대로 보여준다 (승인된 것만 걸러내지 않음).
    const verifyByDate = new Map<string, HabitVerifyResponse>();
    verifies.forEach((v) => verifyByDate.set(v.verifyDate, v));

    const pad = (n: number) => n.toString().padStart(2, "0");
    const cells: Array<{
        day: number;
        dateStr: string;
        isToday: boolean;
        verify?: HabitVerifyResponse;
    } | null> = [];

    for (let i = 0; i < startDayOfWeek; i++) cells.push(null);
    for (let day = 1; day <= daysInMonth; day++) {
        const dateStr = `${year}-${pad(month + 1)}-${pad(day)}`;
        cells.push({
            day,
            dateStr,
            isToday: day === today,
            verify: verifyByDate.get(dateStr),
        });
    }

    return (
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 mb-6">
            <div className="grid grid-cols-3 items-center mb-4">
                <div />
                <div className="flex items-center justify-center gap-2">
                    <CalendarIcon className="w-4 h-4 text-emerald-600" />
                    <h3 className="text-sm font-extrabold text-slate-900">
                        {year}년 {month + 1}월
                    </h3>
                </div>
                <div className="flex items-center justify-end gap-2 text-[11px]">
                    <span className="flex items-center gap-1 text-slate-500">
                        <span className="w-2.5 h-2.5 rounded-sm bg-emerald-500 inline-block" />
                        성공
                    </span>
                    <span className="flex items-center gap-1 text-slate-500">
                        <span className="w-2.5 h-2.5 rounded-sm bg-amber-400 inline-block" />
                        검토중
                    </span>
                    <span className="flex items-center gap-1 text-slate-400">
                        <span className="w-2.5 h-2.5 rounded-sm bg-slate-200 inline-block" />
                        대기
                    </span>
                </div>
            </div>

            <div className="grid grid-cols-7 gap-2 mb-2 text-center text-xs font-bold text-slate-400">
                {weekDays.map((d, i) => (
                    <div key={i} className={i === 0 ? "text-rose-400" : i === 6 ? "text-blue-400" : ""}>
                        {d}
                    </div>
                ))}
            </div>

            <div className="grid grid-cols-7 gap-2">
                {cells.map((cell, idx) => {
                    if (!cell) return <div key={idx} className="h-12 sm:h-14 rounded-xl bg-transparent" />;

                    const { day, isToday, verify } = cell;

                    let bgColor = "bg-slate-50 border-slate-200/70 text-slate-500";
                    let label: React.ReactNode = null;

                    if (verify?.status === "APPROVED") {
                        bgColor =
                            "bg-emerald-500 text-white font-bold border-emerald-600 shadow-xs cursor-pointer hover:bg-emerald-600 transition-all";
                        label = <span className="text-[10px] mt-0.5">성공 ✓</span>;
                    } else if (verify?.status === "PENDING") {
                        bgColor =
                            "bg-amber-50 border-amber-300 text-amber-800 cursor-pointer hover:bg-amber-100 transition-all";
                        label = <span className="text-[9px] mt-0.5">검토중</span>;
                    } else if (verify?.status === "REJECTED") {
                        bgColor =
                            "bg-rose-50 border-rose-300 text-rose-700 cursor-pointer hover:bg-rose-100 transition-all";
                        label = <span className="text-[9px] mt-0.5">반려됨</span>;
                    } else if (isToday) {
                        bgColor = "border-amber-300 ring-2 ring-amber-200 text-amber-900 font-bold";
                        label = <span className="text-[9px] text-amber-600 mt-0.5">오늘</span>;
                    }

                    return (
                        <div
                            key={idx}
                            onClick={() => verify && onSelectDay(verify)}
                            className={`h-12 sm:h-14 rounded-2xl border flex flex-col items-center justify-center relative select-none transition-all ${bgColor}`}
                        >
                            <span className="text-xs">{day}</span>
                            {label}
                        </div>
                    );
                })}
            </div>
            <p className="text-[11px] text-slate-400 mt-3 text-center">
                💡 인증이 등록된 날짜를 클릭하면 사진과 내용을 상세히 볼 수 있습니다.
            </p>
        </div>
    );
}

// ---------- 모달: 오늘 인증하기 ----------

function CertifyModal({
    onClose,
    onSubmit,
}: {
    onClose: () => void;
    onSubmit: (file: File | null, description: string) => Promise<void>;
}) {
    const [file, setFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<string | null>(null);
    const [description, setDescription] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
        const f = e.target.files?.[0] ?? null;
        setFile(f);
        setPreview(f ? URL.createObjectURL(f) : null);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSubmitting(true);
        setError(null);
        try {
            await onSubmit(file, description);
        } catch (err) {
            setError(err instanceof Error ? err.message : "인증 등록에 실패했습니다.");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <ModalShell>
            <ModalHeader title="📸 오늘 인증하기" subtitle="오늘 실천한 내용을 사진과 함께 남겨보세요." onClose={onClose} />
            <form onSubmit={handleSubmit} className="mt-4 space-y-4">
                <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">인증 사진 (선택)</label>
                    <div className="relative w-full h-48 rounded-2xl overflow-hidden bg-slate-100 border border-slate-200 flex items-center justify-center">
                        {preview ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img src={preview} alt="인증 미리보기" className="w-full h-full object-cover" />
                        ) : (
                            <span className="text-xs text-slate-400">사진을 선택해주세요</span>
                        )}
                        <label className="absolute bottom-2 right-2 cursor-pointer px-3 py-1.5 bg-white/90 hover:bg-white text-slate-800 text-xs font-bold rounded-lg shadow-md flex items-center gap-1.5">
                            <Upload className="w-3.5 h-3.5" />
                            <span>사진 업로드</span>
                            <input type="file" accept="image/*" onChange={handleFile} className="hidden" />
                        </label>
                    </div>
                </div>

                <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">인증 내용</label>
                    <textarea
                        rows={3}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="오늘 어떻게 실천했는지 적어주세요."
                        className="w-full px-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 resize-none"
                    />
                </div>

                {error && <p className="text-xs text-rose-500 font-semibold">{error}</p>}

                <ModalActions onClose={onClose} submitting={submitting} submitLabel="인증 등록" />
            </form>
        </ModalShell>
    );
}

// ---------- 모달: 습관 추가 ----------

function CreateHabitModal({
    onClose,
    onSubmit,
}: {
    onClose: () => void;
    onSubmit: (title: string, description: string, days: number) => Promise<void>;
}) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [days, setDays] = useState(3);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) {
            setError("습관 제목은 필수입니다.");
            return;
        }
        setSubmitting(true);
        setError(null);
        try {
            await onSubmit(title.trim(), description.trim(), days);
        } catch (err) {
            setError(err instanceof Error ? err.message : "습관 생성에 실패했습니다.");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <ModalShell>
            <ModalHeader title="🌱 습관 추가하기" subtitle="새로 도전할 습관을 등록해보세요." onClose={onClose} />
            <form onSubmit={handleSubmit} className="mt-4 space-y-4">
                <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                        습관 제목 <span className="text-rose-500">*</span>
                    </label>
                    <input
                        type="text"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        placeholder="예: 매일 30분 운동하기"
                        className="w-full px-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                </div>

                <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">습관 설명</label>
                    <textarea
                        rows={3}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="습관에 대한 설명을 적어주세요."
                        className="w-full px-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 resize-none"
                    />
                </div>

                <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">주간 목표 횟수 (1~7)</label>
                    <input
                        type="number"
                        min={1}
                        max={7}
                        value={days}
                        onChange={(e) => setDays(Number(e.target.value))}
                        className="w-full px-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                </div>

                {error && <p className="text-xs text-rose-500 font-semibold">{error}</p>}

                <ModalActions onClose={onClose} submitting={submitting} submitLabel="습관 생성" />
            </form>
        </ModalShell>
    );
}

// ---------- 모달: 습관 포기 확인 ----------

function HabitFailModal({ onClose, onConfirm }: { onClose: () => void; onConfirm: () => Promise<void> }) {
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleConfirm = async () => {
        setSubmitting(true);
        setError(null);
        try {
            await onConfirm();
        } catch (err) {
            setError(err instanceof Error ? err.message : "처리에 실패했습니다.");
            setSubmitting(false);
        }
    };

    return (
        <ModalShell borderClass="border-rose-200" widthClass="max-w-sm">
            <div className="text-center">
                <div className="w-12 h-12 rounded-full bg-rose-100 text-rose-500 flex items-center justify-center mx-auto mb-3">
                    <AlertTriangle className="w-6 h-6" />
                </div>
                <h3 className="text-base font-bold text-slate-900">습관 포기 확정</h3>
                <p className="text-xs text-slate-500 mt-2 leading-relaxed">
                    이 습관을 실패로 종료할까요?
                    <br />
                    <span className="text-rose-500 font-semibold">(벌칙 수행 기록이 추가됩니다.)</span>
                </p>
                {error && <p className="text-xs text-rose-500 font-semibold mt-2">{error}</p>}
                <div className="mt-6 flex items-center justify-center gap-2">
                    <button
                        onClick={onClose}
                        disabled={submitting}
                        className="px-4 py-2.5 rounded-xl text-xs font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 transition-colors disabled:opacity-50"
                    >
                        취소
                    </button>
                    <button
                        onClick={handleConfirm}
                        disabled={submitting}
                        className="px-4 py-2.5 rounded-xl text-xs font-bold text-white bg-rose-500 hover:bg-rose-600 shadow-md shadow-rose-500/20 transition-colors disabled:opacity-50"
                    >
                        {submitting ? "처리 중..." : "포기 확정"}
                    </button>
                </div>
            </div>
        </ModalShell>
    );
}

// ---------- 모달: 성공 인증 상세 ----------

const CERT_STATUS_META = {
    APPROVED: {
        title: "성공 인증",
        titleClass: "text-emerald-600",
        borderClass: "border-emerald-100",
        btnClass: "bg-emerald-500 hover:bg-emerald-600",
    },
    PENDING: {
        title: "검토 대기 중",
        titleClass: "text-amber-600",
        borderClass: "border-amber-100",
        btnClass: "bg-amber-500 hover:bg-amber-600",
    },
    REJECTED: {
        title: "반려된 인증",
        titleClass: "text-rose-600",
        borderClass: "border-rose-100",
        btnClass: "bg-rose-500 hover:bg-rose-600",
    },
} as const;

function CertDetailModal({ item, onClose }: { item: HabitVerifyResponse; onClose: () => void }) {
    const meta = CERT_STATUS_META[item.status];

    return (
        <ModalShell borderClass={meta.borderClass} widthClass="max-w-sm">
            <ModalHeader
                title={meta.title}
                icon={<CheckCircle2 className="w-5 h-5" />}
                onClose={onClose}
                titleClass={meta.titleClass}
            />
            <div className="mt-3">
                {item.imageUrl && (
                    <div className="w-full h-56 rounded-2xl overflow-hidden bg-slate-100 border border-slate-200 shadow-inner">
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img src={item.imageUrl} alt="인증 사진" className="w-full h-full object-cover" />
                    </div>
                )}
                <div className="mt-3 bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <p className="text-xs font-semibold text-slate-800 leading-relaxed">
                        {item.description || "등록된 내용이 없습니다."}
                    </p>
                </div>
                <p className="mt-3 text-[11px] text-slate-400 text-right">인증 일자: {item.verifyDate}</p>
            </div>
            <button
                onClick={onClose}
                className={`mt-4 w-full py-2.5 text-white text-xs font-bold rounded-xl shadow-xs ${meta.btnClass}`}
            >
                확인
            </button>
        </ModalShell>
    );
}

// ---------- 모달: 벌칙 인증하기 ----------

function PenaltyCertModal({
    onClose,
    onSubmit,
}: {
    onClose: () => void;
    onSubmit: (file: File, description: string) => Promise<void>;
}) {
    const [file, setFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<string | null>(null);
    const [description, setDescription] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
        const f = e.target.files?.[0] ?? null;
        setFile(f);
        setPreview(f ? URL.createObjectURL(f) : null);
        setError(null);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!file) {
            setError("벌칙 수행 사진은 필수입니다.");
            return;
        }
        setSubmitting(true);
        setError(null);
        try {
            await onSubmit(file, description);
        } catch (err) {
            setError(err instanceof Error ? err.message : "벌칙 인증 등록에 실패했습니다.");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <ModalShell borderClass="border-rose-100">
            <ModalHeader
                title="☕ 벌칙 인증하기"
                subtitle="벌칙을 수행한 사진과 내용을 남겨주세요."
                onClose={onClose}
                titleClass="text-rose-600"
            />
            <form onSubmit={handleSubmit} className="mt-4 space-y-4">
                <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                        벌칙 수행 사진 <span className="text-rose-500">*</span>
                    </label>
                    <div className="relative w-full h-48 rounded-2xl overflow-hidden bg-slate-100 border border-rose-200 flex items-center justify-center">
                        {preview ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img src={preview} alt="벌칙 인증 미리보기" className="w-full h-full object-cover" />
                        ) : (
                            <span className="text-xs text-slate-400">사진을 선택해주세요</span>
                        )}
                        <label className="absolute bottom-2 right-2 cursor-pointer px-3 py-1.5 bg-white/90 hover:bg-white text-slate-800 text-xs font-bold rounded-lg shadow-md flex items-center gap-1.5">
                            <Upload className="w-3.5 h-3.5" />
                            <span>사진 업로드</span>
                            <input type="file" accept="image/*" onChange={handleFile} className="hidden" />
                        </label>
                    </div>
                </div>

                <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">인증 내용</label>
                    <textarea
                        rows={3}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="어떻게 벌칙을 수행했는지 적어주세요."
                        className="w-full px-4 py-2 rounded-xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-rose-400 resize-none"
                    />
                </div>

                {error && <p className="text-xs text-rose-500 font-semibold">{error}</p>}

                <ModalActions onClose={onClose} submitting={submitting} submitLabel="벌칙 인증 등록" tone="rose" />
            </form>
        </ModalShell>
    );
}

// ---------- 모달: 벌칙 수행 상세 ----------

function PenaltyDetailModal({ item, onClose }: { item: PenaltyVerifyDetail; onClose: () => void }) {
    return (
        <ModalShell borderClass="border-rose-100" widthClass="max-w-sm">
            <ModalHeader
                title="벌칙 수행 기록"
                icon={<Coffee className="w-5 h-5 text-rose-600" />}
                onClose={onClose}
                titleClass="text-rose-600"
            />
            <div className="mt-3">
                <p className="text-xs font-bold text-slate-500">{item.habitTitle}</p>
                <p className="text-[11px] text-slate-400 mt-0.5">벌칙: {item.penaltyText}</p>
                {item.imageUrl && (
                    <div className="mt-2.5 w-full h-56 rounded-2xl overflow-hidden bg-slate-100 border border-rose-200 shadow-inner">
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img src={item.imageUrl} alt="벌칙 사진" className="w-full h-full object-cover" />
                    </div>
                )}
                <div className="mt-3 bg-rose-50/50 p-3 rounded-xl border border-rose-100">
                    <p className="text-xs font-semibold text-rose-900 leading-relaxed">
                        {item.description || "등록된 내용이 없습니다."}
                    </p>
                </div>
                <p className="mt-3 text-[11px] text-slate-400 text-right">인증 일자: {item.verifyDate ?? "-"}</p>
            </div>
            <button
                onClick={onClose}
                className="mt-4 w-full py-2.5 bg-rose-500 hover:bg-rose-600 text-white text-xs font-bold rounded-xl shadow-xs"
            >
                확인
            </button>
        </ModalShell>
    );
}
