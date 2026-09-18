import { GroupPage } from './GroupPage';

export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <GroupPage roomId={id} />;
}
