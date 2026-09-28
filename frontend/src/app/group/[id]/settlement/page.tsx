import { SettlementEntryPage } from '../../../../views/SettlementEntryPage';

export default async function Page({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ preview?: string }>;
}) {
  const [{ id }, { preview }] = await Promise.all([params, searchParams]);

  return <SettlementEntryPage groupId={id} preview={preview === 'true'} />;
}
