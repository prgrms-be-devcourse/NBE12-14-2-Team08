import { MemberPage } from "./MemberPage";

export default async function Page({
    params,
}: {
    params: Promise<{ id: string; memberId: string }>;
}) {
    const { id, memberId } = await params;

    return <MemberPage groupId={id} groupMemberId={memberId} />;
}
