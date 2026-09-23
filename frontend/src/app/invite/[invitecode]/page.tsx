import Link from "next/link";

export default async function Page({
    params,
}: {
    params: Promise<{ invitecode: string }>;
}) {
    const { invitecode } = await params;

    return (
        <main className="p-8 space-y-4">
            <h1 className="text-xl font-bold">초대 코드: {invitecode}</h1>
            <nav className="flex flex-col gap-2">
                <Link href="/main" className="underline">
                    메인으로
                </Link>
            </nav>
        </main>
    );
}
