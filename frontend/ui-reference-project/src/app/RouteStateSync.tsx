'use client';

import { useEffect } from 'react';
import { useApp, PageType } from '../context/AppContext';

type RouteStateSyncProps = {
  page: PageType;
  roomId?: string;
  habitId?: string;
};

export function RouteStateSync({ page, roomId, habitId }: RouteStateSyncProps) {
  const {
    rooms,
    setCurrentRoomId,
    setSelectedUserId,
    syncPageFromRoute,
  } = useApp();

  useEffect(() => {
    syncPageFromRoute(page);
    if (roomId) setCurrentRoomId(roomId);

    if (page === 'habit-detail' && roomId && habitId) {
      const room = rooms.find((item) => item.id === roomId);
      const member = room?.members.find((item) => item.habit?.id === habitId);
      if (member) setSelectedUserId(member.userId);
    }
  }, [page, roomId, habitId, rooms, setCurrentRoomId, setSelectedUserId, syncPageFromRoute]);

  return null;
}
