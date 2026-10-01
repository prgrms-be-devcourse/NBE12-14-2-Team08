import { AdminRoute } from './AdminRoute';

export default async function Page({
  params,
}: {
  params: Promise<{ groupId: string }>;
}) {
  const { groupId } = await params;
  return <AdminRoute groupId={groupId} />;
}
