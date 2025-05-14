import { useEffect, useState } from "react";
import axios from "axios";
import "./App.css";

const API_URL = "http://localhost:8080/project/events";

function App() {
    const [events, setEvents] = useState([]);
    const [newEvent, setNewEvent] = useState({
        name: "",
    });
    const [filterName, setFilterName] = useState("");
    const [currentEvent, setCurrentEvent] = useState(null);
    const [isUpdating, setIsUpdating] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchEvents();
    }, []);

    const fetchEvents = async () => {
        try {
            const response = await axios.get(API_URL);
            console.log("Date primite de la server:", response.data);

            if (Array.isArray(response.data)) {
                setEvents(response.data);
            } else if (response.data && Array.isArray(response.data._embedded?.events)) {
                setEvents(response.data._embedded.events);
            } else {
                console.error("Datele primite nu sunt în formatul așteptat:", response.data);
                setEvents([]);
            }
        } catch (err) {
            console.error("Eroare la încărcarea evenimentelor:", err);
            setError("Nu s-au putut încărca evenimentele");
            setEvents([]);
        }
    };
    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setNewEvent({
            ...newEvent,
            [name]: value
        });
    };

    const handleAddEvent = async (e) => {
        e.preventDefault();
        try {
            await axios.post(API_URL, newEvent);
            setNewEvent({ name: ""}); // Reset form
            fetchEvents();
        } catch (err) {
            console.error("Eroare la adăugarea evenimentului:", err);
            setError("Nu s-a putut adăuga evenimentul");
        }
    };

    const handleUpdateClick = (event) => {
        setCurrentEvent(event);
        setIsUpdating(true);
    };

    const handleUpdateEvent = async () => {
        try {
            await axios.put(`${API_URL}/${currentEvent.id}`, currentEvent);
            setIsUpdating(false);
            fetchEvents();
        } catch (err) {
            console.error("Eroare la actualizarea evenimentului:", err);
            setError("Nu s-a putut actualiza evenimentul");
        }
    };

    const handleDeleteEvent = async (id) => {
        try {
            await axios.delete(`${API_URL}/${id}`);
            fetchEvents();
        } catch (err) {
            console.error("Eroare la ștergerea evenimentului:", err);
            setError("Nu s-a putut șterge evenimentul");
        }
    };

    const filteredEvents = events.filter(event => {
        return (
            event.name.toLowerCase().includes(filterName.toLowerCase())
        );
    });

    return (
        <div className="container">
            <h1>Evenimente</h1>

            {/* Formular Adăugare */}
            <h2>Adaugă Eveniment</h2>
            <form onSubmit={handleAddEvent} className="add-form">
                <input
                    type="text"
                    placeholder="Nume Eveniment"
                    name="name"
                    value={newEvent.name}
                    onChange={handleInputChange}
                    required
                />
                {/* Adaugă alte câmpuri după necesitate */}
                <button type="submit" className="btn-add">Adaugă</button>
            </form>

            {/* Secțiunea de Filtrare */}
            <h2>Toate Evenimentele</h2>
            <div className="filter-section">
                <button className="btn-filter">Filtrează</button>
                <input
                    type="text"
                    placeholder="Nume Eveniment"
                    value={filterName}
                    onChange={(e) => setFilterName(e.target.value)}
                />
            </div>

            {/* Lista de Evenimente */}
            <div className="events-list">
                {filteredEvents.map(event => (
                    <div key={event.id} className="event-item">
                        <div className="event-name">{event.name}</div>
                        <div className="event-actions">
                            <button
                                className="btn-delete"
                                onClick={() => handleDeleteEvent(event.id)}
                            >
                                Delete
                            </button>
                            <button
                                className="btn-update"
                                onClick={() => handleUpdateClick(event)}
                            >
                                Update
                            </button>
                        </div>
                    </div>
                ))}
            </div>

            {/* Modal de Update */}
            {isUpdating && (
                <div className="modal-backdrop">
                    <div className="modal">
                        <h2>Actualizează Eveniment</h2>
                        <input
                            type="text"
                            placeholder="Nume Eveniment"
                            value={currentEvent.name}
                            onChange={(e) => setCurrentEvent({...currentEvent, name: e.target.value})}
                            required
                        />
                        {}
                        <div className="modal-actions">
                            <button onClick={() => setIsUpdating(false)}>Anulează</button>
                            <button onClick={handleUpdateEvent}>Salvează</button>
                        </div>
                    </div>
                </div>
            )}

            {/* Afișare Erori */}
            {error && <div className="error-message">{error}</div>}
        </div>
    );
}

export default App;