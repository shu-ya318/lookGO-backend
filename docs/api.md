# TrtcGo - Backend API Specification

## Shared Error Response Format

```json
{
  "error": {
    "message": "Detailed error message"
  }
}
```

---

### Users

#### Create User

- **Endpoint**: `/users/create-user`
- **Request Body**:

```json
{
  "user": {
    "email": "user@example.com",
    "password": "password123",
    "name": "test"
  }
}
```

- **Response (200 OK)**:

```json
{
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "test",
    "tier_id": 1,
    "role_id": 1,
    "status": 1,
    "created_at": "2023-01-01T00:00:00Z",
    "updated_at": "2023-01-01T00:00:00Z"
  }
}
```

#### Login User

- **Endpoint**: `/users/log-in-user`
- **Request Body**:

```json
{
  "credentials": {
    "email": "user@example.com",
    "password": "password123"
  }
}
```

- **Response (200 OK)**:

```json
{
  "token": "jwt_token_string",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "test",
    "last_login_at": "2023-01-01T00:00:00Z"
  }
}
```

#### Get User Profile

- **Endpoint**: `/users/get-profile`
- **Request Body**:

```json
{}
```

- **Response (200 OK)**:

```json
{
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "test",
    "birth_date": "1990-01-01",
    "tier_id": 1,
    "role_id": 1,
    "status": 1,
    "created_at": "2023-01-01T00:00:00Z",
    "updated_at": "2023-01-01T00:00:00Z",
    "last_login_at": "2023-01-01T00:00:00Z"
  }
}
```

#### Update User Profile

- **Endpoint**: `/users/update-profile`
- **Request Body**:

```json
{
  "user": {
    "name": "newName",
    "birth_date": "1990-06-15",
    "password": "newpassword123"
  }
}
```

- **Response (200 OK)**:

```json
{
  "user": {
    "id": 1,
    "email": "user@example.com",
    "name": "newName",
    "birth_date": "1990-06-15",
    "tier_id": 1,
    "role_id": 1,
    "status": 1,
    "updated_at": "2023-01-02T00:00:00Z"
  }
}
```

---

### Stations

#### Get Station List

- **Endpoint**: `/stations/get-list`
- **Request Body**:

```json
{}
```

- **Response (200 OK)**:

```json
{
  "stations": [
    {
      "id": 1,
      "name_zh_tw": "台北車站",
      "name_en": "Taipei Main Station",
      "status": 1
    }
  ]
}
```

#### Get Station Details

Includes basic station info, facility availability (direct columns on `stations`), line-station mappings (`lines_stations`), and exits (`station_exits`).

- **Endpoint**: `/stations/get-details`
- **Request Body**:

```json
{
  "station": {
    "id": 1
  }
}
```

- **Response (200 OK)**:

```json
{
  "station": {
    "id": 1,
    "name_zh_tw": "台北車站",
    "name_en": "Taipei Main Station",
    "status": 1,
    "atm": "available",
    "nursing_room": "available",
    "diaper_table": "not available",
    "charging_station": "available",
    "ticket_machine": "available",
    "locker": "not available",
    "drinking_water": "available",
    "restroom": "available",
    "lines": [
      {
        "line_id": 1,
        "station_code": "BL12",
        "station_sequence": 12,
        "cumulative_distance": 8420.5,
        "cumulative_time": 900
      }
    ],
    "exits": [
      {
        "id": 1,
        "name_zh_tw": "1號出口",
        "name_en": "Exit 1",
        "elevator": "available",
        "escalator": "not available",
        "status": 1
      }
    ]
  }
}
```

#### Query Fares

- **Endpoint**: `/stations/get-fares`
- **Request Body**:

```json
{
  "fare_query": {
    "from_station_id": 1,
    "to_station_id": 2
  }
}
```

- **Response (200 OK)**:

```json
{
  "fares": [
    {
      "fare_type": 1,
      "price": 20
    }
  ]
}
```

#### Query Travel Times

- **Endpoint**: `/stations/get-travel-times`
- **Request Body**:

```json
{
  "travel_time_query": {
    "from_station_id": 1,
    "to_station_id": 2
  }
}
```

- **Response (200 OK)**:

```json
{
  "travel_times": [
    {
      "line_id": 1,
      "travel_seconds": 900
    }
  ]
}
```

---

### User Station Bookmarks

#### Add Bookmark

- **Endpoint**: `/user-station-bookmarks/create-bookmark`
- **Request Body**:

```json
{
  "bookmark": {
    "station_id": 1
  }
}
```

- **Response (200 OK)**:

```json
{
  "bookmark": {
    "id": 1,
    "user_id": 1,
    "station_id": 1,
    "created_at": "2023-01-01T00:00:00Z"
  }
}
```

#### Get Bookmark List

- **Endpoint**: `/user-station-bookmarks/get-list`
- **Request Body**:

```json
{}
```

- **Response (200 OK)**:

```json
{
  "bookmarks": [
    {
      "id": 1,
      "station_id": 1,
      "created_at": "2023-01-01T00:00:00Z"
    }
  ]
}
```

#### Delete Bookmark

- **Endpoint**: `/user-station-bookmarks/delete-bookmark`
- **Request Body**:

```json
{
  "bookmark": {
    "id": 1
  }
}
```

- **Response (200 OK)**:

```json
{
  "success": true
}
```

---

### User Trip Plans

#### Create Trip Plan

- **Endpoint**: `/user-trip-plans/create-plan`
- **Request Body**:

```json
{
  "trip_plan": {
    "origin_station_id": 1,
    "destination_station_id": 5,
    "fare_type": 1,
    "fare_price": 30,
    "transfer_count": 1,
    "routing_strategy": 1,
    "notes": "Commute route"
  }
}
```

- **Response (200 OK)**:

```json
{
  "trip_plan": {
    "id": 1,
    "user_id": 1,
    "origin_station_id": 1,
    "destination_station_id": 5,
    "fare_type": 1,
    "fare_price": 30,
    "transfer_count": 1,
    "routing_strategy": 1,
    "notes": "Commute route",
    "created_at": "2023-01-01T00:00:00Z"
  }
}
```

#### Get Trip Plan List

- **Endpoint**: `/user-trip-plans/get-list`
- **Request Body**:

```json
{}
```

- **Response (200 OK)**:

```json
{
  "trip_plans": [
    {
      "id": 1,
      "origin_station_id": 1,
      "destination_station_id": 5,
      "fare_type": 1,
      "fare_price": 30,
      "notes": "Commute route",
      "created_at": "2023-01-01T00:00:00Z"
    }
  ]
}
```

#### Get Trip Plan Details

- **Endpoint**: `/user-trip-plans/get-plan`
- **Request Body**:

```json
{
  "trip_plan": {
    "id": 1
  }
}
```

- **Response (200 OK)**:

```json
{
  "trip_plan": {
    "id": 1,
    "user_id": 1,
    "origin_station_id": 1,
    "destination_station_id": 5,
    "fare_type": 1,
    "fare_price": 30,
    "transfer_count": 1,
    "routing_strategy": 1,
    "notes": "Commute route",
    "created_at": "2023-01-01T00:00:00Z"
  }
}
```

#### Update Trip Plan

- **Endpoint**: `/user-trip-plans/update-plan`
- **Request Body**:

```json
{
  "trip_plan": {
    "id": 1,
    "notes": "Updated notes"
  }
}
```

- **Response (200 OK)**:

```json
{
  "trip_plan": {
    "id": 1,
    "user_id": 1,
    "origin_station_id": 1,
    "destination_station_id": 5,
    "fare_type": 1,
    "fare_price": 30,
    "transfer_count": 1,
    "routing_strategy": 1,
    "notes": "Updated notes",
    "created_at": "2023-01-01T00:00:00Z"
  }
}
```

#### Delete Trip Plan

- **Endpoint**: `/user-trip-plans/delete-plan`
- **Request Body**:

```json
{
  "trip_plan": {
    "id": 1
  }
}
```

- **Response (200 OK)**:

```json
{
  "success": true
}
```

---

### Station Chat Messages

#### Send Message

- **Endpoint**: `/station-chat-messages/create-message`
- **Request Body**:

```json
{
  "message": {
    "station_id": 1,
    "chat_type": 1,
    "content": "Where is the lost and found center?"
  }
}
```

- **Response (200 OK)**:

```json
{
  "message": {
    "id": 1,
    "station_id": 1,
    "user_id": 1,
    "chat_type": 1,
    "content": "Where is the lost and found center?",
    "created_at": "2023-01-01T12:00:00Z"
  }
}
```

#### Get Message List

- **Endpoint**: `/station-chat-messages/get-list`
- **Request Body**:

```json
{
  "query": {
    "station_id": 1,
    "limit": 50,
    "offset": 0
  }
}
```

- **Response (200 OK)**:

```json
{
  "messages": [
    {
      "id": 1,
      "station_id": 1,
      "user_id": 1,
      "chat_type": 1,
      "content": "Where is the lost and found center?",
      "created_at": "2023-01-01T12:00:00Z"
    }
  ]
}
```
