import { InviteRoute } from './InviteRoute';

export default async function Page({
  params,
}: {
  params: Promise<{ invitecode: string }>;
}) {
  const { invitecode } = await params;
  return <InviteRoute invitecode={invitecode} />;
}
