'use client';

import { fromRouteId } from '../../../../../lib/routeIds';
import { RouteStateSync } from '../../../../RouteStateSync';
import { HabitDetailPage } from '../../../../../views/HabitDetailPage';

export function HabitPage({ roomId, habitId }: { roomId: string; habitId: string }) {
  return (
    <>
      <RouteStateSync
        page="habit-detail"
        roomId={fromRouteId(roomId, 'room')}
        habitId={fromRouteId(habitId, 'habit')}
      />
      <HabitDetailPage />
    </>
  );
}
