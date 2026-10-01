import { HabitPage } from './HabitPage';

export default async function Page({
  params,
}: {
  params: Promise<{ id: string; habitId: string }>;
}) {
  const { id, habitId } = await params;
  return <HabitPage roomId={id} habitId={habitId} />;
}
