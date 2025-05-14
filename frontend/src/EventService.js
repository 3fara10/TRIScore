// src/EventService.js
import axios from "axios";

const API_URL = "http://localhost:8080/project/events";

export const getAllEvents = () => axios.get(API_URL);
export const getEventById = (id) => axios.get(`${API_URL}/${id}`);
export const createEvent = (event) => axios.post(API_URL, event);
export const updateEvent = (id, event) => axios.put(`${API_URL}/${id}`, event);
export const deleteEvent = (id) => axios.delete(`${API_URL}/${id}`);
