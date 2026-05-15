package com.simulatedtez.gochat.remote.api_interfaces

import com.simulatedtez.gochat.model.response.ConversationSummary
import com.simulatedtez.gochat.model.response.GroupChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.remote.api_usecases.CreateGroupChatParams

interface IChatApiService {
    suspend fun createChatRoom(params: CreateChatRoomParams): IResponse<ParentResponse<String>>
    suspend fun createConversations(params: CreateConversationsParams): IResponse<ParentResponse<String>>
    suspend fun deleteConversation(chatReference: String): IResponse<ParentResponse<String>>
    suspend fun fetchUserConversations(username: String): IResponse<ParentResponse<List<ConversationSummary>>>
    suspend fun createGroupChat(params: CreateGroupChatParams): IResponse<ParentResponse<GroupChatResponse>>
}
