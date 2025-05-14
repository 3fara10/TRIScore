import { useEffect, useState } from "react";

function EventList() {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetch("http://localhost:8080/project/events")
            .then((res) => res.json())
            .then((data) => {
                setEvents(data);
                setLoading(false);
            })
            .catch((err) => {
                console.error("Eroare la fetch:", err);
                setLoading(false);
            });
    }, []);

    if (loading) return <p>Se încarcă...</p>;

    return (
        <div>
            <h2>Evenimente:</h2>
            <ul>
                {events.map((ev) => (
                    <li key={ev.id}>{ev.name}</li>
                ))}
            </ul>
        </div>
    );
}

export default EventList;
