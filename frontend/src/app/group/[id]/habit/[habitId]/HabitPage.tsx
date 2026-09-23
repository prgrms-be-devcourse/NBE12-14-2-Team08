"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import {
    ArrowLeft,
    Calendar as CalendarIcon,
    CheckCircle2,
    Camera,
    Coffee,
    Crown,
    AlertTriangle,
    Upload,
    X,
} from "lucide-react";
import {
    ApiError,
    getMe,
    getHabit,
    getGroupMember,
    getHabitVerifications,
    getPenaltiesByGroupMember,
    getPenaltyDetail,
    createHabitVerification,
    getHabitUploadUrl,
    getPenaltyUploadUrl,
    submitPenalty,
    uploadFileToSignedUrl,
    failHabit,
    type MemberResponse,
    type HabitResponse,
    type GroupMemberDetail,
    type HabitVerifyResponse,
    type PenaltyVerifySummary,
    type PenaltyVerifyDetail,
} from "@/lib/api";

type Props = { groupId: string; habitId: string };

export function HabitPage({ groupId, habitId }: Props) {
    const [me, setMe] = useState<MemberResponse | null>(null);
    const [habit, setHabit] = useState<HabitResponse | null>(null);
    const [member, setMember] = useState<GroupMemberDetail | null>(null);
    const [verifies, setVerifies] = useState<HabitVerifyResponse[]>([]);
    const [penalties, setPenalties] = useState<PenaltyVerifySummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<ApiError | Error | null>(null);

    const [showCertifyModal, setShowCertifyModal] = useState(false);
    const [showPenaltyModal, setShowPenaltyModal] = useState(false);
    const [showFailModal, setShowFailModal] = useState(false);
    const [certDetail, setCertDetail] = useState<HabitVerifyResponse | null>(null);
    const [penaltyDetail, setPenaltyDetail] = useState<PenaltyVerifyDetail | null>(null);

    const load = useCallback(async () => {
        try {
            setError(null);
            const meRes = await getMe();
            const habitRes = await getHabit(habitId);
            const memberRes = await getGroupMember(groupId, habitRes.groupMemberId);

            const isOwnHabit = meRes.memberId === memberRes.memberId;

            const [verifiesRes, penaltiesRes] = await Promise.all([
                isOwnHabit ? getHabitVerifications(habitId) : Promise.resolve([]),
                getPenaltiesByGroupMember(groupId, memberRes.groupMemberId),
            ]);

            setMe(meRes);
            setHabit(habitRes);
            setMember(memberRes);
            setVerifies(verifiesRes);
            setPenalties(penaltiesRes);
        } catch (err) {
            setError(err instanceof Error ? err : new Error("알 수 없는 오류가 발생했습니다."));
        } finally {
            setLoading(false);
        }
    }, [groupId, habitId]);

    useEffect(() => {
        load();
    }, [load]);

    if (loading) {
        return <CenteredMessage text="불러오는 중..." />;
    }

    if (error) {
        if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
            return (
                <CenteredMessage text="로그인이 필요합니다.">
                    <Link href="/login" className="underline text-emerald-600 font-semibold">
                        로그인 하러 가기
                    </Link>
                </CenteredMessage>
            );
        }
        return <CenteredMessage text={error.message} />;
    }

    if (!habit || !member || !me) {
        return <CenteredMessage text="습관 정보를 찾을 수 없습니다." />;
    }

    const isMe = me.memberId === member.memberId;
    const isHost = member.role === "OWNER";

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
                    습관 상세 페이지
                </span>
            </div>

            {/* 1. 프로필 & 누적 벌칙 */}
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

                {/* 2. 습관 정보 & 액션 버튼 */}
                <div className="mt-5 pt-5 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div>
                        <span className="text-[11px] font-bold text-emerald-600 uppercase">도전 습관</span>
                        <h2 className="text-lg font-extrabold text-slate-900">{habit.title}</h2>
                        <p className="text-xs text-slate-500 mt-0.5">
                            {habit.description || "매일매일 실천하고 성공을 기록해보세요!"}
                        </p>
                    </div>

                    {isMe && habit.status === "ACTIVE" && (
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
            </div>

            {/* 3. 캘린더 */}
            {isMe && <VerifyCalendar verifies={verifies} onSelectDay={setCertDetail} />}

            {/* 4. 실천 및 벌칙 기록 */}
            <FeedList
                verifies={isMe ? verifies : []}
                penalties={penalties}
                isMe={isMe}
                onSelectCert={setCertDetail}
                onSelectPenalty={async (p) => {
                    try {
                        const detail = await getPenaltyDetail(p.id);
                        setPenaltyDetail(detail);
                    } catch {
                        // 상세 조회 실패 시 조용히 무시 (요약 정보는 이미 화면에 있음)
                    }
                }}
                onNeedPenalty={() => setShowPenaltyModal(true)}
            />

            {/* 모달들 */}
            {showCertifyModal && (
                <CertifyModal
                    onClose={() => setShowCertifyModal(false)}
                    onSubmit={async (file, description) => {
                        let imageUrl: string | null = null;
                        if (file) {
                            const { uploadUrl, publicUrl } = await getHabitUploadUrl(habitId, file.name);
                            await uploadFileToSignedUrl(uploadUrl, file);
                            imageUrl = publicUrl;
                        }
                        await createHabitVerification(habitId, { description: description || null, imageUrl });
                        setShowCertifyModal(false);
                        await load();
                    }}
                />
            )}

            {showPenaltyModal && (
                <PenaltyCertModal
                    onClose={() => setShowPenaltyModal(false)}
                    onSubmit={async (file, description) => {
                        const { uploadUrl, publicUrl } = await getPenaltyUploadUrl(file.name);
                        await uploadFileToSignedUrl(uploadUrl, file);
                        await submitPenalty(habitId, { description: description || null, imageUrl: publicUrl });
                        setShowPenaltyModal(false);
                        await load();
                    }}
                />
            )}

            {showFailModal && (
                <HabitFailModal
                    onClose={() => setShowFailModal(false)}
                    onConfirm={async () => {
                        await failHabit(habitId);
                        setShowFailModal(false);
                        await load();
                    }}
                />
            )}

            {certDetail && <CertDetailModal item={certDetail} onClose={() => setCertDetail(null)} />}
            {penaltyDetail && (
                <PenaltyDetailModal item={penaltyDetail} onClose={() => setPenaltyDetail(null)} />
            )}
        </div>
    );
}

function CenteredMessage({ text, children }: { text: string; children?: React.ReactNode }) {
    return (
        <div className="max-w-4xl mx-auto px-4 py-16 text-center space-y-3">
            <p className="text-slate-500">{text}</p>
            {children}
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

    const approvedByDate = new Map<string, HabitVerifyResponse>();
    verifies.forEach((v) => {
        if (v.status === "APPROVED") approvedByDate.set(v.verifyDate, v);
    });

    const pad = (n: number) => n.toString().padStart(2, "0");
    const cells: Array<{
        day: number;
        dateStr: string;
        isSuccess: boolean;
        isPast: boolean;
        isToday: boolean;
        verify?: HabitVerifyResponse;
    } | null> = [];

    for (let i = 0; i < startDayOfWeek; i++) cells.push(null);
    for (let day = 1; day <= daysInMonth; day++) {
        const dateStr = `${year}-${pad(month + 1)}-${pad(day)}`;
        cells.push({
            day,
            dateStr,
            isSuccess: approvedByDate.has(dateStr),
            isPast: day < today,
            isToday: day === today,
            verify: approvedByDate.get(dateStr),
        });
    }

    return (
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 mb-6">
            <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                    <CalendarIcon className="w-4 h-4 text-emerald-600" />
                    <h3 className="text-sm font-extrabold text-slate-900">
                        {year}년 {month + 1}월 인증 캘린더
                    </h3>
                </div>
                <div className="flex items-center gap-2 text-[11px]">
                    <span className="flex items-center gap-1 text-slate-500">
                        <span className="w-2.5 h-2.5 rounded-sm bg-emerald-500 inline-block" />
                        성공
                    </span>
                    <span className="flex items-center gap-1 text-slate-500">
                        <span className="w-2.5 h-2.5 rounded-sm bg-rose-400 inline-block" />
                        미인증
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

                    const { day, isSuccess, isPast, isToday, verify } = cell;
                    let bgColor = "bg-slate-50 border-slate-200/70 text-slate-500";
                    if (isSuccess) {
                        bgColor =
                            "bg-emerald-500 text-white font-bold border-emerald-600 shadow-xs cursor-pointer hover:bg-emerald-600 transition-all";
                    } else if (isPast) {
                        bgColor = "bg-rose-50 border-rose-200 text-rose-700";
                    } else if (isToday) {
                        bgColor = "bg-amber-50 border-amber-300 text-amber-900 font-bold ring-2 ring-amber-200";
                    }

                    return (
                        <div
                            key={idx}
                            onClick={() => verify && onSelectDay(verify)}
                            className={`h-12 sm:h-14 rounded-2xl border flex flex-col items-center justify-center relative select-none transition-all ${bgColor}`}
                        >
                            <span className="text-xs">{day}</span>
                            {isSuccess ? (
                                <span className="text-[10px] mt-0.5">성공 ✓</span>
                            ) : isPast ? (
                                <span className="text-[9px] text-rose-400 mt-0.5">미인증</span>
                            ) : isToday ? (
                                <span className="text-[9px] text-amber-600 mt-0.5">오늘</span>
                            ) : null}
                        </div>
                    );
                })}
            </div>
            <p className="text-[11px] text-slate-400 mt-3 text-center">
                💡 초록색 성공 날짜를 클릭하면 등록된 인증 사진과 내용을 상세히 볼 수 있습니다.
            </p>
        </div>
    );
}

// ---------- 실천 및 벌칙 기록 ----------

function FeedList({
    verifies,
    penalties,
    isMe,
    onSelectCert,
    onSelectPenalty,
    onNeedPenalty,
}: {
    verifies: HabitVerifyResponse[];
    penalties: PenaltyVerifySummary[];
    isMe: boolean;
    onSelectCert: (v: HabitVerifyResponse) => void;
    onSelectPenalty: (p: PenaltyVerifySummary) => void;
    onNeedPenalty: () => void;
}) {
    type Row =
        | { kind: "habit"; key: string; date: string; verify: HabitVerifyResponse }
        | { kind: "penalty"; key: string; date: string; penalty: PenaltyVerifySummary };

    const rows: Row[] = [
        ...verifies.map((v) => ({
            kind: "habit" as const,
            key: `h-${v.id}`,
            date: v.verifyDate,
            verify: v,
        })),
        ...penalties.map((p) => ({
            kind: "penalty" as const,
            key: `p-${p.id}`,
            // 아직 제출 전(REQUIRED)이라 날짜가 없는 벌칙은 맨 위로 오도록 미래 날짜 취급
            date: p.verifyDate ?? "9999-99-99",
            penalty: p,
        })),
    ].sort((a, b) => b.date.localeCompare(a.date));

    if (rows.length === 0) {
        return (
            <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6">
                <h3 className="text-sm font-extrabold text-slate-900 mb-2">실천 및 벌칙 기록</h3>
                <p className="text-xs text-slate-400">아직 기록이 없습니다.</p>
            </div>
        );
    }

    return (
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6">
            <h3 className="text-sm font-extrabold text-slate-900 mb-4 flex items-center justify-between">
                <span>실천 및 벌칙 기록</span>
                <span className="text-xs font-normal text-slate-400">최근 실천 타임라인</span>
            </h3>

            <div className="space-y-3">
                {rows.map((row) => (
                    <div
                        key={row.key}
                        className="p-3.5 sm:p-4 rounded-2xl border border-slate-100 bg-slate-50/60 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                    >
                        {row.kind === "habit" ? (
                            <>
                                <div>
                                    <div className="flex items-center gap-2 mb-1">
                                        <span className="text-xs font-bold text-slate-500">{row.verify.verifyDate}</span>
                                        <span className="text-xs font-extrabold text-slate-900">습관 인증</span>
                                    </div>
                                    <p className="text-[11px] text-slate-400">
                                        {row.verify.description || "실천 내역이 등록되지 않았습니다."}
                                    </p>
                                </div>
                                <div className="flex items-center gap-2 shrink-0">
                                    {row.verify.status === "APPROVED" && (
                                        <button
                                            onClick={() => onSelectCert(row.verify)}
                                            className="flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-100 hover:bg-emerald-200 px-3 py-1.5 rounded-xl transition-colors cursor-pointer"
                                        >
                                            <CheckCircle2 className="w-3.5 h-3.5" />
                                            <span>인증완료</span>
                                        </button>
                                    )}
                                    {row.verify.status === "PENDING" && (
                                        <span className="text-xs font-bold text-amber-700 bg-amber-100 px-3 py-1.5 rounded-xl">
                                            검토 대기
                                        </span>
                                    )}
                                    {row.verify.status === "REJECTED" && (
                                        <span className="text-xs font-bold text-rose-600 bg-rose-100 px-3 py-1.5 rounded-xl">
                                            반려됨
                                        </span>
                                    )}
                                </div>
                            </>
                        ) : (
                            <>
                                <div>
                                    <div className="flex items-center gap-2 mb-1">
                                        <span className="text-xs font-bold text-slate-500">
                                            {row.penalty.verifyDate ?? "미제출"}
                                        </span>
                                        <span className="text-xs font-extrabold text-slate-900">{row.penalty.habitTitle}</span>
                                    </div>
                                    <p className="text-[11px] text-slate-400">벌칙 대상 기록입니다.</p>
                                </div>
                                <div className="flex items-center gap-2 shrink-0">
                                    <span className="text-xs font-bold text-rose-600 px-2.5 py-1 rounded-xl bg-rose-100">
                                        미인증
                                    </span>
                                    {row.penalty.status === "REQUIRED" ? (
                                        isMe && (
                                            <button
                                                onClick={onNeedPenalty}
                                                className="flex items-center gap-1 text-xs font-bold text-white bg-rose-500 hover:bg-rose-600 px-3 py-1.5 rounded-xl shadow-xs transition-all cursor-pointer"
                                            >
                                                <Coffee className="w-3.5 h-3.5" />
                                                <span>벌칙 수행하기</span>
                                            </button>
                                        )
                                    ) : (
                                        <button
                                            onClick={() => onSelectPenalty(row.penalty)}
                                            className="flex items-center gap-1 text-xs font-bold text-rose-700 bg-rose-100 hover:bg-rose-200 px-3 py-1.5 rounded-xl transition-colors cursor-pointer"
                                        >
                                            <Coffee className="w-3.5 h-3.5" />
                                            <span>벌칙완료 확인</span>
                                        </button>
                                    )}
                                </div>
                            </>
                        )}
                    </div>
                ))}
            </div>
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

function CertDetailModal({ item, onClose }: { item: HabitVerifyResponse; onClose: () => void }) {
    return (
        <ModalShell borderClass="border-emerald-100" widthClass="max-w-sm">
            <ModalHeader
                title="성공 인증"
                icon={<CheckCircle2 className="w-5 h-5 text-emerald-600" />}
                onClose={onClose}
                titleClass="text-emerald-600"
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
                className="mt-4 w-full py-2.5 bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold rounded-xl shadow-xs"
            >
                확인
            </button>
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

// ---------- 모달 공통 조각 ----------

function ModalShell({
    children,
    borderClass = "border-slate-100",
    widthClass = "max-w-md",
}: {
    children: React.ReactNode;
    borderClass?: string;
    widthClass?: string;
}) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
            <div className={`bg-white w-full ${widthClass} rounded-3xl shadow-2xl border ${borderClass} p-6 max-h-[90vh] overflow-y-auto`}>
                {children}
            </div>
        </div>
    );
}

function ModalHeader({
    title,
    subtitle,
    icon,
    onClose,
    titleClass = "text-slate-900",
}: {
    title: string;
    subtitle?: string;
    icon?: React.ReactNode;
    onClose: () => void;
    titleClass?: string;
}) {
    return (
        <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div>
                <h2 className={`text-lg font-bold flex items-center gap-1.5 ${titleClass}`}>
                    {icon}
                    <span>{title}</span>
                </h2>
                {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
            </div>
            <button onClick={onClose} className="p-1 rounded-full hover:bg-slate-100 text-slate-400">
                <X className="w-5 h-5" />
            </button>
        </div>
    );
}

function ModalActions({
    onClose,
    submitting,
    submitLabel,
    tone = "emerald",
}: {
    onClose: () => void;
    submitting: boolean;
    submitLabel: string;
    tone?: "emerald" | "rose";
}) {
    const toneClass = tone === "rose" ? "bg-rose-500 hover:bg-rose-600" : "bg-emerald-500 hover:bg-emerald-600";
    return (
        <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
            <button
                type="button"
                onClick={onClose}
                disabled={submitting}
                className="px-4 py-2 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-100 disabled:opacity-50"
            >
                취소
            </button>
            <button
                type="submit"
                disabled={submitting}
                className={`px-5 py-2 rounded-xl text-sm font-bold text-white shadow-sm disabled:opacity-50 ${toneClass}`}
            >
                {submitting ? "처리 중..." : submitLabel}
            </button>
        </div>
    );
}
