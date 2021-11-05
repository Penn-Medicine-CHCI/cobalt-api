package com.cobaltplatform.ic.model;

import com.cobaltplatform.ic.backend.model.response.DispositionResponse;
import com.cobaltplatform.ic.backend.model.response.FlagResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SortByFlagTest {
   @Test void sortedFlagSuccess() {
       var response1  = new DispositionResponse().setFlag(FlagResponse.forDispositionFlag(DispositionFlag.COORDINATE_REFERRAL));
       var response2  = new DispositionResponse().setFlag(FlagResponse.forDispositionFlag(DispositionFlag.NEEDS_INITIAL_SAFETY_PLANNING));
       var response3  = new DispositionResponse().setFlag(FlagResponse.forDispositionFlag(DispositionFlag.NEEDS_FURTHER_ASSESSMENT_WITH_MHIC));
       var response4  = new DispositionResponse().setFlag(FlagResponse.forDispositionFlag(DispositionFlag.NEEDS_SAFETY_PLANNING_FOLLOW));

       var unsorted = List.of(response1, response2, response4, response3);

       var sorted = List.of(response2, response4, response3, response1);

       var sortedResponse = unsorted.stream().sorted(new SortByFlag()).collect(Collectors.toList());
       assertEquals(sortedResponse, sorted);
   }
}