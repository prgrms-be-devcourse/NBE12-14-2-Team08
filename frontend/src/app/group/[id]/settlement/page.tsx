import { SettlementEntryPage } from '../../../../views/SettlementEntryPage';

export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  return <SettlementEntryPage groupId={id} />;
}
