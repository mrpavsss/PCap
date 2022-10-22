package com.example.pcap.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pcap.MainActivity
import com.example.pcap.Post
import com.example.pcap.PostAdapter
import com.example.pcap.R
import com.example.pcap.EndlessRecyclerViewScrollListener
import com.parse.FindCallback
import com.parse.ParseException
import com.parse.ParseQuery
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

open class FeedFragment : Fragment() {

    lateinit var rvPosts: RecyclerView
    lateinit var adapter: PostAdapter
    lateinit var scrollListener: EndlessRecyclerViewScrollListener
    lateinit var swipeContainer: SwipeRefreshLayout

    var allPosts: MutableList<Post> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPosts = view.findViewById(R.id.rvPosts)

        adapter = PostAdapter(requireContext(), allPosts)
        rvPosts.adapter = adapter

        rvPosts.layoutManager = LinearLayoutManager(requireContext())

        scrollListener = object : EndlessRecyclerViewScrollListener(rvPosts.layoutManager as LinearLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadNextDataFromApi(page)
            }
        }
        // Adds the scroll listener to RecyclerView
        rvPosts.addOnScrollListener(scrollListener)
        swipeContainer = view.findViewById(R.id.swipeContainer)
        // Setup refresh listener which triggers new data loading

        swipeContainer.setOnRefreshListener {
            // Your code to refresh the list here.
            swipeContainer.setRefreshing(false)
            // once the network request has completed successfully.
            fetchQueryAsync(0)
        }

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light);

        queryPosts()
    }

    open fun loadNextDataFromApi(offset: Int) {
        // Send an API request to retrieve appropriate paginated data
        //  --> Send the request including an offset value (i.e `page`) as a query parameter.
        //  --> Deserialize and construct new model objects from the API response
        //  --> Append the new data objects to the existing set of items inside the array of items
        //  --> Notify the adapter of the new items made with `notifyItemRangeInserted()`
        getNextPageOfPosts(offset)
    }
    // Query for all posts in our server
    open fun queryPosts() {

        // Specify which class to query
        val query: ParseQuery<Post> = ParseQuery.getQuery(Post::class.java)
        // Find all Post objects
        query.include(Post.KEY_USER)
        query.addDescendingOrder("createdAt")
        query.setLimit(postLimit)
        query.findInBackground(object : FindCallback<Post> {
            override fun done(posts: MutableList<Post>?, e: ParseException?) {
                if (e != null) {
                    Log.e(TAG, "Error fetching posts")
                } else {
                    if (posts != null) {
                        for (post in posts) {
                            Log.i(
                                TAG, "Post: " + post.getDescription()
                                        + " , username: " + post.getUser())
                        }

                        allPosts.addAll(posts)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

        })
    }

    open fun getNextPageOfPosts(offset: Int) {
        // Specify which class to query
        val query: ParseQuery<Post> = ParseQuery.getQuery(Post::class.java)
        // Find all Post objects
        query.include(Post.KEY_USER)
        query.addDescendingOrder("createdAt")
        query.setLimit(postLimit)
        query.setSkip(postLimit*offset)
        query.findInBackground(object : FindCallback<Post> {
            override fun done(posts: MutableList<Post>?, e: ParseException?) {
                if (e != null) {
                    Log.e(TAG, "Error fetching posts")
                } else {
                    if (posts != null) {
                        for (post in posts) {
                            Log.i(
                                TAG, "Post: " + post.getDescription()
                                        + " , username: " + post.getUser())
                        }

                        allPosts.addAll(posts)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

        })
    }
    fun fetchQueryAsync(page: Int) {
        // Send the network request to fetch the updated data
        queryPosts()
        adapter.clear()
        adapter.addAll(allPosts)
        swipeContainer.setRefreshing(false)
    }

    companion object {
        const val TAG = "FeedFragment"
        private const val postLimit = 20
    }
}