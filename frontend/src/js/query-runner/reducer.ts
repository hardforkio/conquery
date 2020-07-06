import { toUpperCaseUnderscore } from "../common/helpers";
import * as actionTypes from "./actionTypes";
import type {
  DatasetIdT,
  GetQueryResponseDoneT,
  GetQueryResponseT,
  ColumnDescription,
} from "../api/types";

interface APICallType {
  loading?: boolean;
  success?: boolean;
  error?: string;
}

export interface QueryRunnerStateT {
  runningQuery: number | string | null;
  queryRunning: boolean;
  startQuery: APICallType;
  stopQuery: APICallType;
  queryResult:
    | (APICallType & {
        datasetId?: string;
        resultCount?: number;
        resultUrl?: string;
        resultColumns?: ColumnDescription[];
      })
    | null;
}

export default function createQueryRunnerReducer(type: string) {
  const initialState: QueryRunnerStateT = {
    runningQuery: null,
    queryRunning: false,
    startQuery: {},
    stopQuery: {},
    queryResult: null,
  };

  const capitalType = toUpperCaseUnderscore(type);

  // Example1: START_STANDARD_QUERY_START
  // Example2: START_TIMEBASED_QUERY_START
  const START_QUERY_START = actionTypes[`START_${capitalType}_QUERY_START`];
  const START_QUERY_SUCCESS = actionTypes[`START_${capitalType}_QUERY_SUCCESS`];
  const START_QUERY_ERROR = actionTypes[`START_${capitalType}_QUERY_ERROR`];
  const STOP_QUERY_START = actionTypes[`STOP_${capitalType}_QUERY_START`];
  const STOP_QUERY_SUCCESS = actionTypes[`STOP_${capitalType}_QUERY_SUCCESS`];
  const STOP_QUERY_ERROR = actionTypes[`STOP_${capitalType}_QUERY_ERROR`];
  const QUERY_RESULT_START = actionTypes[`QUERY_${capitalType}_RESULT_START`];
  const QUERY_RESULT_RESET = actionTypes[`QUERY_${capitalType}_RESULT_RESET`];
  const QUERY_RESULT_SUCCESS =
    actionTypes[`QUERY_${capitalType}_RESULT_SUCCESS`];
  const QUERY_RESULT_ERROR = actionTypes[`QUERY_${capitalType}_RESULT_ERROR`];

  const getQueryResult = (
    data: GetQueryResponseDoneT,
    datasetId: DatasetIdT
  ) => {
    return {
      datasetId,
      loading: false,
      success: true,
      error: null,
      resultCount: data.numberOfResults,
      resultUrl: data.resultUrl,
      resultColumns: data.columnDescriptions,
    };
  };

  const getQueryError = (
    data: GetQueryResponseT,
    message: string | null
  ): string => {
    if (message) {
      return message;
    }

    if (data.status === "CANCELED") {
      return "queryRunner.queryCanceled";
    }

    return "queryRunner.queryFailed";
  };

  return (
    state: QueryRunnerStateT = initialState,
    action: Object
  ): QueryRunnerStateT => {
    switch (action.type) {
      // To start a query
      case START_QUERY_START:
        return {
          ...state,
          stopQuery: {},
          startQuery: { loading: true },
          queryResult: null,
        };
      case START_QUERY_SUCCESS:
        return {
          ...state,
          runningQuery: action.payload.data.id,
          queryRunning: true,
          stopQuery: {},
          startQuery: { success: true },
        };
      case START_QUERY_ERROR:
        return {
          ...state,
          stopQuery: {},
          startQuery: {
            error: action.payload.message || action.payload.status,
          },
        };

      // To cancel a query
      case STOP_QUERY_START:
        return { ...state, startQuery: {}, stopQuery: { loading: true } };
      case STOP_QUERY_SUCCESS:
        return {
          ...state,
          runningQuery: null,
          queryRunning: false,
          startQuery: {},
          stopQuery: { success: true },
        };
      case STOP_QUERY_ERROR:
        return {
          ...state,
          startQuery: {},
          stopQuery: { error: action.payload.message || action.payload.status },
        };

      // To check for query results
      case QUERY_RESULT_START:
        return { ...state, queryResult: { loading: true } };
      case QUERY_RESULT_RESET:
        return { ...state, queryResult: { loading: false } };
      case QUERY_RESULT_SUCCESS:
        const queryResult = getQueryResult(
          action.payload.data,
          action.payload.datasetId
        );

        return {
          ...state,
          queryResult,
          runningQuery: null,
          queryRunning: false,
        };
      case QUERY_RESULT_ERROR:
        const error = getQueryError(
          // TODO Clean up. This is just a quick fix to prevent the frontend to fail if the backend sends the status of a failed query
          {status: action.payload.status}, 
          action.payload.message
        );

        return {
          ...state,
          runningQuery: null,
          queryRunning: false,
          queryResult: {
            loading: false,
            error,
          },
        };
      default:
        return state;
    }
  };
}
