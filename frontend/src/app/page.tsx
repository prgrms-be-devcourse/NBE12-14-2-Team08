export default async function Home() {
    const response = await fetch("http://localhost:8080/api/connection-test", {
        cache: "no-store",
    });

    return <main>{await response.text()}</main>;
}