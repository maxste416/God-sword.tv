package com.godsword.tv.models

import android.os.Parcel
import android.os.Parcelable

data class Video(
    val id: String,
    val title: String,
    val description: String,
    val duration: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val category: String,
    var isFavorite: Boolean = false
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(duration)
        parcel.writeString(thumbnailUrl)
        parcel.writeString(videoUrl)
        parcel.writeString(category)
        parcel.writeByte(if (isFavorite) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Video> {
        override fun createFromParcel(parcel: Parcel): Video {
            return Video(parcel)
        }

        override fun newArray(size: Int): Array<Video?> {
            return arrayOfNulls(size)
        }
    }
}