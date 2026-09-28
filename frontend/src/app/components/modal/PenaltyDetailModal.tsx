'use client';

import React from 'react';

interface PenaltyDetailModalProps {
  isOpen: boolean;
  onClose: () => void;
  habitTitle: string;
  penaltyText: string;
  imageUrl: string;
  description: string;
  verifyDate: string;
  onApprove?: () => void;
  onReject?: () => void;
}

export default function PenaltyDetailModal({
                                             isOpen,
                                             onClose,
                                             habitTitle,
                                             penaltyText,
                                             imageUrl,
                                             description,
                                             verifyDate,
                                             onApprove,
                                             onReject,
                                           }: PenaltyDetailModalProps) {
  if (!isOpen) return null;

  return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
        <div className="relative w-full max-w-sm rounded-[32px] border-2 border-black bg-white p-6 shadow-2xl">
          <button
              onClick={onClose}
              className="absolute right-5 top-5 text-gray-400 hover:text-black font-bold text-lg cursor-pointer"
          >
            ✕
          </button>

          {/* 상단: 스냅샷 벌칙 문구 및 습관명 */}
          <div className="text-center pt-2">
            <div className="inline-flex items-center gap-1.5 rounded-full bg-rose-50 px-3 py-1 border border-rose-200/60 mb-2">
              <span className="text-[11px] font-bold text-rose-600">벌칙 :</span>
              <span className="text-[11px] font-extrabold text-rose-900">{penaltyText || '벌칙 미지정'}</span>
            </div>
            <p className="text-base font-extrabold text-gray-900">{habitTitle}</p>
          </div>

          {/* 이미지 영역 */}
          <div className="mt-4 flex justify-center">
            <div className="h-56 w-56 overflow-hidden rounded-[28px] border-2 border-black bg-gray-50 shadow-inner">
              <img
                  src={imageUrl || '/placeholder.png'}
                  alt="벌칙 인증 사진"
                  className="h-full w-full object-cover"
              />
            </div>
          </div>

          {/* 설명 박스 */}
          <div className="mt-4 rounded-2xl border-2 border-black p-3.5 bg-gray-50">
            <p className="text-xs text-gray-700 min-h-[40px] whitespace-pre-wrap font-medium">
              {description || '작성된 벌칙 수행 내용이 없습니다.'}
            </p>
          </div>

          {/* 인증 날짜 */}
          <div className="mt-4 text-center text-xs font-bold text-gray-600">
            인증 날짜: {verifyDate}
          </div>

          {/* 모달 하단 승인 / 거절 버튼 영역 */}
          {(onApprove || onReject) && (
              <div className="mt-5 flex gap-2">
                {onApprove && (
                    <button
                        onClick={() => {
                          onApprove();
                          onClose();
                        }}
                        className="flex-1 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white font-extrabold text-xs shadow-sm transition-all cursor-pointer"
                    >
                      승인
                    </button>
                )}
                {onReject && (
                    <button
                        onClick={() => {
                          onReject();
                          onClose();
                        }}
                        className="flex-1 py-2.5 rounded-xl bg-white hover:bg-rose-50 text-gray-600 hover:text-rose-600 border border-gray-300 hover:border-rose-300 font-extrabold text-xs transition-all cursor-pointer"
                    >
                      거절
                    </button>
                )}
              </div>
          )}
        </div>
      </div>
  );
}