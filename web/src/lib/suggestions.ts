import { Timestamp, addDoc, collection } from "firebase/firestore";
import { db } from "@/lib/firebase";

const suggestionsCol = collection(db, "suggestions");

export async function submitSuggestion(params: {
  message: string;
  name?: string;
  contact?: string;
}): Promise<void> {
  const message = params.message.trim();

  if (!message) {
    throw new Error("Please write a suggestion before sending.");
  }

  if (message.length > 2000) {
    throw new Error("Suggestions are limited to 2000 characters.");
  }

  await addDoc(suggestionsCol, {
    message,
    name: (params.name ?? "").trim(),
    contact: (params.contact ?? "").trim(),
    createdAt: Timestamp.now(),
  });
}
