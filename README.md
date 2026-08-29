# Monthly Meal Calendar

A lightweight, mobile-friendly meal tracker for recording daily breakfasts, lunches, and dinners. Data is stored in Firebase Firestore and syncs in real time across devices.

**Live site:** [adarsh0047.github.io/meal-tracker](https://adarsh0047.github.io/meal-tracker/)

## Features

- Monthly calendar with Breakfast, Lunch, and Dinner checkboxes
- Real-time Firestore synchronization
- Monthly meal-count summary
- Per-month prices for breakfast, lunch, dinner, and delivery
- Monthly cost calculation and all-month history
- Stacked bar chart comparing monthly costs by meal type and delivery
- Responsive dark-mode interface

## Run locally

This is a static site with no build step or package installation required.

1. Clone the repository.
2. Serve the project directory with any static web server, for example:

   ```bash
   python3 -m http.server 8000
   ```

3. Open `http://localhost:8000` in a browser.

The Firebase project configuration is currently included in `index.html`.

## Data model

The app stores data in the Firestore `trackers` collection using two documents:

| Document | Key format | Stored value |
| --- | --- | --- |
| Meal data | `YYYY-MM-DD` | `{ breakfast, lunch, dinner }` boolean values |
| Monthly costs | `YYYY-MM` | `{ breakfast, lunch, dinner, delivery }` numeric values |

Delivery is a flat cost for the selected month. Monthly totals combine completed meals at that month's prices with its delivery cost.

## Deployment

The repository is published through GitHub Pages. Pushes to `main` update the deployed static site.

## Notes

- Firestore security rules control who can access or modify the data.
- The client-side document IDs are identifiers, not a security boundary; protect the data with appropriate Firebase rules.
